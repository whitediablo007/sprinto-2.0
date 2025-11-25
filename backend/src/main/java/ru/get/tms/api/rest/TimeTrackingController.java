package ru.get.tms.api.rest;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.dto.timeentry.TimeEntryDTO;
import ru.get.tms.service.TimeTrackingService;

/**
 * REST контроллер для управления учетом времени и таймерами.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>POST /api/time-entries/timer/start - запуск таймера на задаче
 *   <li>POST /api/time-entries/timer/stop - остановка активного таймера
 *   <li>GET /api/time-entries/timer/active - получение активного таймера
 *   <li>POST /api/time-entries - создание ручной записи времени
 *   <li>GET /api/time-entries/my - получение всех записей времени текущего пользователя
 *   <li>GET /api/time-entries/task/{taskId} - получение записей времени для задачи
 *   <li>DELETE /api/time-entries/{id} - удаление записи времени (soft delete)
 * </ul>
 *
 * <p>Реализует требования FR-002 (таймеры), FR-003 (учет времени).
 */
@Slf4j
@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeTrackingController {

  private final TimeTrackingService timeTrackingService;

  /**
   * Запускает таймер на указанной задаче.
   *
   * <p>У пользователя может быть только один активный таймер. Если таймер уже запущен, вернется
   * ошибка 409 CONFLICT.
   *
   * @param request запрос с ID задачи
   * @param authentication информация о текущем пользователе
   * @return Mono с созданной записью времени (активный таймер)
   */
  @PostMapping("/timer/start")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<TimeEntryDTO> startTimer(
      @Valid @RequestBody StartTimerRequest request, Authentication authentication) {
    UUID currentUserId = extractUserId(authentication);
    log.debug("Starting timer for user={}, task={}", currentUserId, request.getTaskId());

    return timeTrackingService.startTimer(request.getTaskId(), currentUserId);
  }

  /**
   * Останавливает активный таймер текущего пользователя.
   *
   * <p>Рассчитывает duration и cost на основе elapsed time и hourly rate.
   *
   * @param authentication информация о текущем пользователе
   * @return Mono с остановленной записью времени
   */
  @PostMapping("/timer/stop")
  public Mono<TimeEntryDTO> stopTimer(Authentication authentication) {
    UUID currentUserId = extractUserId(authentication);
    log.debug("Stopping timer for user={}", currentUserId);

    return timeTrackingService.stopTimer(currentUserId);
  }

  /**
   * Получает активный таймер текущего пользователя.
   *
   * <p>Возвращает пустой ответ (204 NO_CONTENT), если активного таймера нет.
   *
   * @param authentication информация о текущем пользователе
   * @return Mono с активным таймером или пустой Mono
   */
  @GetMapping("/timer/active")
  public Mono<TimeEntryDTO> getActiveTimer(Authentication authentication) {
    UUID currentUserId = extractUserId(authentication);
    log.debug("Getting active timer for user={}", currentUserId);

    return timeTrackingService.getActiveTimer(currentUserId);
  }

  /**
   * Создает ручную запись времени.
   *
   * <p>Используется для добавления времени вручную (не через таймер). Требует указания start_time и
   * end_time.
   *
   * @param dto данные записи времени
   * @param authentication информация о текущем пользователе
   * @return Mono с созданной записью времени
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<TimeEntryDTO> createManualEntry(
      @Valid @RequestBody TimeEntryDTO dto, Authentication authentication) {
    UUID currentUserId = extractUserId(authentication);
    log.debug("Creating manual time entry for user={}, task={}", currentUserId, dto.getTaskId());

    return timeTrackingService.createManualEntry(dto, currentUserId);
  }

  /**
   * Получает все записи времени текущего пользователя.
   *
   * <p>Исключает удаленные записи (soft delete).
   *
   * @param authentication информация о текущем пользователе
   * @return Flux записей времени
   */
  @GetMapping("/my")
  public Flux<TimeEntryDTO> getMyTimeEntries(Authentication authentication) {
    UUID currentUserId = extractUserId(authentication);
    log.debug("Getting time entries for user={}", currentUserId);

    return timeTrackingService.getUserTimeEntries(currentUserId);
  }

  /**
   * Получает записи времени для указанной задачи.
   *
   * <p>Исключает удаленные записи (soft delete).
   *
   * @param taskId ID задачи
   * @return Flux записей времени
   */
  @GetMapping("/task/{taskId}")
  public Flux<TimeEntryDTO> getTaskTimeEntries(@PathVariable UUID taskId) {
    log.debug("Getting time entries for task={}", taskId);

    return timeTrackingService.getTaskTimeEntries(taskId);
  }

  /**
   * Удаляет запись времени (soft delete).
   *
   * <p>Нельзя удалить активный таймер - сначала нужно его остановить.
   *
   * @param id ID записи времени
   * @param request запрос с причиной удаления (опционально)
   * @param authentication информация о текущем пользователе
   * @return Mono пустой при успехе
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteTimeEntry(
      @PathVariable UUID id,
      @RequestBody(required = false) DeleteTimeEntryRequest request,
      Authentication authentication) {
    UUID currentUserId = extractUserId(authentication);
    String reason = request != null ? request.getReason() : null;

    log.debug("Deleting time entry: id={}, user={}, reason='{}'", id, currentUserId, reason);

    return timeTrackingService.deleteTimeEntry(id, currentUserId, reason);
  }

  /**
   * Извлекает ID пользователя из Authentication.
   *
   * @param authentication объект аутентификации
   * @return UUID пользователя
   */
  private UUID extractUserId(Authentication authentication) {
    // TODO: В реальной реализации извлечь userId из JWT claims
    // Временно возвращаем mock UUID для компиляции
    return UUID.fromString((String) authentication.getPrincipal());
  }

  /** DTO для запроса запуска таймера. */
  public static class StartTimerRequest {
    private UUID taskId;

    public UUID getTaskId() {
      return taskId;
    }

    public void setTaskId(UUID taskId) {
      this.taskId = taskId;
    }
  }

  /** DTO для запроса удаления записи времени. */
  public static class DeleteTimeEntryRequest {
    private String reason;

    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }
  }
}
