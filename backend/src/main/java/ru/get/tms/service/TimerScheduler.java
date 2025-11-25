package ru.get.tms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.get.tms.domain.project.Project;
import ru.get.tms.domain.task.Task;
import ru.get.tms.domain.timeentry.TimeEntry;
import ru.get.tms.dto.timeentry.TimerUpdateDTO;
import ru.get.tms.repository.ProjectRepository;
import ru.get.tms.repository.TaskRepository;
import ru.get.tms.repository.TimeEntryRepository;

/**
 * Scheduler для отправки WebSocket обновлений активных таймеров.
 *
 * <p>Выполняется каждую секунду (fixedDelay = 1000ms). Для каждого активного таймера отправляет
 * обновление состояния через WebSocket.
 *
 * <p>Реализует требования FR-001, FR-002 (real-time обновления таймера с latency ≤1 секунда).
 *
 * <p>Использует реактивный подход для неблокирующей работы. В будущем будет интегрирован с
 * WebSocket handler для отправки через /user/queue/timer.
 *
 * <p>TODO: Интегрировать с WebSocketHandler для отправки TimerUpdateDTO клиентам.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimerScheduler {

  private final TimeEntryRepository timeEntryRepository;
  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;

  /**
   * Обновляет активные таймеры каждую секунду.
   *
   * <p>Находит все активные таймеры, для каждого рассчитывает elapsed time и cost, готовит
   * обновление для отправки через WebSocket.
   *
   * <p>Метод выполняется асинхронно в реактивном scheduler для неблокирующей работы.
   */
  @Scheduled(fixedDelay = 1000) // Каждую секунду
  public void updateActiveTimers() {
    timeEntryRepository
        .findAll()
        .filter(TimeEntry::isActiveTimer)
        .flatMap(this::buildTimerUpdate)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            this::processTimerUpdate,
            error -> log.error("Error updating active timers: {}", error.getMessage(), error));
  }

  /**
   * Строит DTO с обновлением таймера.
   *
   * <p>Загружает связанные Task и Project для enrichment данных. Рассчитывает elapsed time и
   * current cost.
   *
   * @param timeEntry активная запись времени (таймер)
   * @return Mono с TimerUpdateDTO
   */
  private Mono<TimerUpdateDTO> buildTimerUpdate(TimeEntry timeEntry) {
    return taskRepository
        .findById(timeEntry.getTaskId())
        .flatMap(
            task ->
                projectRepository
                    .findById(task.getProjectId())
                    .map(project -> createTimerUpdateDTO(timeEntry, task, project)))
        .onErrorResume(
            error -> {
              log.warn(
                  "Failed to build timer update for timeEntryId={}: {}",
                  timeEntry.getId(),
                  error.getMessage());
              return Mono.empty();
            });
  }

  /**
   * Создает TimerUpdateDTO с рассчитанным elapsed time и cost.
   *
   * @param timeEntry запись времени (таймер)
   * @param task задача
   * @param project проект
   * @return TimerUpdateDTO
   */
  private TimerUpdateDTO createTimerUpdateDTO(TimeEntry timeEntry, Task task, Project project) {
    LocalDateTime now = LocalDateTime.now();
    Duration elapsed = Duration.between(timeEntry.getStartTime(), now);
    long elapsedSeconds = elapsed.getSeconds();

    // Рассчитываем текущую стоимость
    String formattedCost = "0.00";
    if (timeEntry.getHourlyRate() != null) {
      BigDecimal hours =
          BigDecimal.valueOf(elapsedSeconds)
              .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
      BigDecimal currentCost =
          hours.multiply(timeEntry.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);
      formattedCost = currentCost.toString();
    }

    return TimerUpdateDTO.builder()
        .timeEntryId(timeEntry.getId())
        .taskId(task.getId())
        .taskTitle(task.getTitle())
        .projectId(project.getId())
        .projectName(project.getName())
        .projectColor(project.getColor())
        .startTime(timeEntry.getStartTime())
        .elapsedSeconds(elapsedSeconds)
        .formattedCost(formattedCost)
        .timestamp(now)
        .build();
  }

  /**
   * Обрабатывает обновление таймера.
   *
   * <p>В текущей реализации просто логирует. В будущем здесь будет интеграция с WebSocket handler
   * для отправки обновлений клиентам через /user/queue/timer.
   *
   * <p>TODO: Реализовать отправку через WebSocketHandler когда будет создан в T056.
   *
   * @param timerUpdate DTO с обновлением таймера
   */
  private void processTimerUpdate(TimerUpdateDTO timerUpdate) {
    log.trace(
        "Timer update prepared: timeEntryId={}, elapsedSeconds={}",
        timerUpdate.getTimeEntryId(),
        timerUpdate.getElapsedSeconds());

    // TODO: Интегрировать с WebSocketHandler для отправки клиентам
    // Пример: webSocketHandler.sendTimerUpdate(timerUpdate);
  }
}
