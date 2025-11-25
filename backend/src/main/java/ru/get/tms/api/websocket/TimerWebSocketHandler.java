package ru.get.tms.api.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import ru.get.tms.dto.timeentry.TimerUpdateDTO;

/**
 * Reactive WebSocket handler для real-time обновлений активных таймеров.
 *
 * <p>Обрабатывает WebSocket соединения на endpoint /ws/timer. Для каждого пользователя создается
 * отдельный Sink, через который отправляются обновления состояния таймера каждую секунду.
 *
 * <p>Интеграция с {@link ru.get.tms.service.TimerScheduler} для получения обновлений.
 *
 * <p>Реализует требования FR-002 (real-time обновления таймера с latency ≤1 секунда).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimerWebSocketHandler implements WebSocketHandler {

  private final ObjectMapper objectMapper;

  /**
   * Map userId -> Sink для индивидуальной отправки обновлений таймера каждому пользователю.
   *
   * <p>ConcurrentHashMap для thread-safe доступа из scheduler и WebSocket threads.
   */
  private final Map<UUID, Sinks.Many<TimerUpdateDTO>> userSinks = new ConcurrentHashMap<>();

  /**
   * Обрабатывает входящее WebSocket соединение для таймера пользователя.
   *
   * <p>Извлекает userId из параметров запроса (?userId=...) и создает индивидуальный stream
   * обновлений для этого пользователя.
   *
   * @param session WebSocket session
   * @return Mono<Void> completion signal
   */
  @Override
  public Mono<Void> handle(WebSocketSession session) {
    // Извлекаем userId из query parameters
    String userIdParam = session.getHandshakeInfo().getUri().getQuery();
    UUID userId = extractUserIdFromQuery(userIdParam);

    if (userId == null) {
      log.warn("WebSocket connection rejected: missing or invalid userId parameter");
      return session.close();
    }

    log.info("WebSocket timer connection established for user: {}", userId);

    // Создаем Sink для этого пользователя
    Sinks.Many<TimerUpdateDTO> userSink = Sinks.many().multicast().onBackpressureBuffer();
    userSinks.put(userId, userSink);

    // Отправляем обновления таймера клиенту
    Mono<Void> output =
        session.send(
            userSink
                .asFlux()
                .map(this::serializeTimerUpdate)
                .map(session::textMessage)
                .doOnError(
                    error ->
                        log.error(
                            "Error sending timer update to user {}: {}",
                            userId,
                            error.getMessage())));

    // Обработка входящих сообщений (если нужно, например, для heartbeat/ping)
    Mono<Void> input =
        session
            .receive()
            .map(msg -> msg.getPayloadAsText())
            .doOnNext(msg -> log.trace("Received WebSocket message from user {}: {}", userId, msg))
            .then();

    // Cleanup при отключении
    return Mono.zip(input, output)
        .doFinally(
            signalType -> {
              log.info(
                  "WebSocket timer connection closed for user: {} (signal: {})",
                  userId,
                  signalType);
              userSinks.remove(userId);
              userSink.tryEmitComplete();
            })
        .then();
  }

  /**
   * Отправляет обновление таймера конкретному пользователю.
   *
   * <p>Вызывается из {@link ru.get.tms.service.TimerScheduler} каждую секунду для активных
   * таймеров.
   *
   * @param userId ID пользователя
   * @param timerUpdate DTO с обновлением таймера
   */
  public void sendTimerUpdate(UUID userId, TimerUpdateDTO timerUpdate) {
    Sinks.Many<TimerUpdateDTO> sink = userSinks.get(userId);

    if (sink != null) {
      Sinks.EmitResult result = sink.tryEmitNext(timerUpdate);

      if (result.isFailure()) {
        log.warn("Failed to emit timer update for user {}: {}", userId, result);
      } else {
        log.trace("Timer update sent to user: {}", userId);
      }
    } else {
      log.trace("No active WebSocket connection for user: {}", userId);
    }
  }

  /**
   * Извлекает userId из query string (?userId=...).
   *
   * @param query query string параметры
   * @return UUID пользователя или null если не найден/невалиден
   */
  private UUID extractUserIdFromQuery(String query) {
    if (query == null || !query.contains("userId=")) {
      return null;
    }

    try {
      String userIdStr = query.substring(query.indexOf("userId=") + 7);
      // Обрезаем до первого & если есть другие параметры
      int ampersandIndex = userIdStr.indexOf('&');
      if (ampersandIndex > 0) {
        userIdStr = userIdStr.substring(0, ampersandIndex);
      }
      return UUID.fromString(userIdStr);
    } catch (Exception e) {
      log.warn("Failed to parse userId from query: {}", query, e);
      return null;
    }
  }

  /**
   * Сериализует TimerUpdateDTO в JSON строку.
   *
   * @param timerUpdate DTO для сериализации
   * @return JSON строка
   */
  private String serializeTimerUpdate(TimerUpdateDTO timerUpdate) {
    try {
      return objectMapper.writeValueAsString(timerUpdate);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize timer update: {}", e.getMessage(), e);
      return "{\"error\":\"serialization_failed\"}";
    }
  }

  /**
   * Получает количество активных WebSocket соединений.
   *
   * @return количество подключенных пользователей
   */
  public int getActiveConnectionsCount() {
    return userSinks.size();
  }

  /**
   * Проверяет, подключен ли пользователь к WebSocket.
   *
   * @param userId ID пользователя
   * @return true если подключен
   */
  public boolean isUserConnected(UUID userId) {
    return userSinks.containsKey(userId);
  }
}
