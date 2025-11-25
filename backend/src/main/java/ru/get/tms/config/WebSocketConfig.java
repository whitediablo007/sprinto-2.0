package ru.get.tms.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import ru.get.tms.api.websocket.NotificationWebSocketHandler;

/**
 * Reactive WebSocket configuration for WebFlux. Uses non-blocking, event-driven WebSocket API
 * compatible with Netty.
 */
@Configuration
public class WebSocketConfig {

  /** Maps WebSocket endpoints to their handlers. Add new WebSocket endpoints here as needed. */
  @Bean
  public HandlerMapping webSocketHandlerMapping(
      NotificationWebSocketHandler notificationHandler,
      ru.get.tms.api.websocket.TimerWebSocketHandler timerHandler) {
    Map<String, WebSocketHandler> map = new HashMap<>();
    // WebSocket endpoint for real-time notifications
    map.put("/ws/notifications", notificationHandler);
    // WebSocket endpoint for real-time timer updates
    map.put("/ws/timer", timerHandler);

    return new SimpleUrlHandlerMapping(map, 1);
  }

  /**
   * Adapter for WebSocket handlers in reactive applications. Required for WebSocket support in
   * WebFlux.
   */
  @Bean
  public WebSocketHandlerAdapter handlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}
