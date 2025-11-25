package ru.get.tms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.timeentry.TimeEntry;
import ru.get.tms.domain.timeentry.TimeEntryType;
import ru.get.tms.dto.timeentry.TimeEntryDTO;
import ru.get.tms.exception.BusinessLogicException;
import ru.get.tms.exception.ConflictException;
import ru.get.tms.exception.ResourceNotFoundException;
import ru.get.tms.mapper.TimeEntryMapper;
import ru.get.tms.repository.ProjectRepository;
import ru.get.tms.repository.TaskRepository;
import ru.get.tms.repository.TimeEntryRepository;
import ru.get.tms.repository.UserRepository;

/**
 * Service для управления учетом времени и таймерами.
 *
 * <p>Реализует бизнес-логику запуска/остановки таймеров, расчета биллинга, управления записями
 * времени.
 *
 * <p>Ключевые правила:
 *
 * <ul>
 *   <li>У пользователя может быть только один активный таймер
 *   <li>При остановке таймера автоматически рассчитывается duration и cost
 *   <li>Ставка биллинга: приоритет проект > пользователь
 *   <li>Cost и hourlyRate immutable после создания записи (для исторической корректности)
 * </ul>
 *
 * <p>Следует реактивному паттерну обработки ошибок (NFR-027).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimeTrackingService {

  private final TimeEntryRepository timeEntryRepository;
  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final TimeEntryMapper timeEntryMapper;

  /**
   * Запускает таймер на указанной задаче.
   *
   * <p>Проверяет, что у пользователя нет активного таймера. Определяет hourlyRate из настроек
   * проекта или пользователя.
   *
   * @param taskId ID задачи
   * @param userId ID пользователя
   * @return Mono с созданной записью времени (активный таймер)
   * @throws ConflictException если у пользователя уже есть активный таймер
   * @throws ResourceNotFoundException если задача не найдена
   */
  @Transactional
  public Mono<TimeEntryDTO> startTimer(UUID taskId, UUID userId) {
    log.debug("Starting timer for userId={}, taskId={}", userId, taskId);

    return checkNoActiveTimer(userId)
        .then(validateTask(taskId))
        .then(determineHourlyRate(taskId, userId))
        .flatMap(
            hourlyRate -> {
              TimeEntry timeEntry =
                  TimeEntry.builder()
                      .taskId(taskId)
                      .userId(userId)
                      .startTime(LocalDateTime.now())
                      .endTime(null) // Активный таймер
                      .entryType(TimeEntryType.TIMER)
                      .hourlyRate(hourlyRate)
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build();

              return timeEntryRepository
                  .save(timeEntry)
                  .map(timeEntryMapper::toDTO)
                  .doOnSuccess(
                      te ->
                          log.info(
                              "Timer started: timeEntryId={}, userId={}, taskId={}",
                              te.getId(),
                              userId,
                              taskId));
            });
  }

  /**
   * Останавливает активный таймер пользователя.
   *
   * <p>Рассчитывает duration и cost на основе elapsed time и hourlyRate. Записывает completedAt.
   *
   * @param userId ID пользователя
   * @return Mono с остановленной записью времени
   * @throws ResourceNotFoundException если активный таймер не найден
   */
  @Transactional
  public Mono<TimeEntryDTO> stopTimer(UUID userId) {
    log.debug("Stopping timer for userId={}", userId);

    return timeEntryRepository
        .findActiveTimerByUserId(userId)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException("No active timer found for user: " + userId)))
        .flatMap(
            timeEntry -> {
              LocalDateTime endTime = LocalDateTime.now();
              timeEntry.setEndTime(endTime);

              // Рассчитываем duration в секундах
              Duration duration = Duration.between(timeEntry.getStartTime(), endTime);
              int durationSeconds = (int) duration.getSeconds();
              timeEntry.setDurationSeconds(durationSeconds);

              // Рассчитываем cost: (durationSeconds / 3600) * hourlyRate
              if (timeEntry.getHourlyRate() != null) {
                BigDecimal hours =
                    BigDecimal.valueOf(durationSeconds)
                        .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
                BigDecimal cost =
                    hours.multiply(timeEntry.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);
                timeEntry.setCost(cost);
              }

              timeEntry.setUpdatedAt(LocalDateTime.now());

              return timeEntryRepository.save(timeEntry);
            })
        .map(timeEntryMapper::toDTO)
        .doOnSuccess(
            te ->
                log.info(
                    "Timer stopped: timeEntryId={}, userId={}, duration={}s, cost={}",
                    te.getId(),
                    userId,
                    te.getDurationSeconds(),
                    te.getCost()));
  }

  /**
   * Получает активный таймер пользователя.
   *
   * @param userId ID пользователя
   * @return Mono с активным таймером или пустой Mono
   */
  public Mono<TimeEntryDTO> getActiveTimer(UUID userId) {
    log.debug("Getting active timer for userId={}", userId);

    return timeEntryRepository
        .findActiveTimerByUserId(userId)
        .map(timeEntryMapper::toDTO)
        .doOnSuccess(
            te -> {
              if (te != null) {
                log.debug("Active timer found for userId={}: timeEntryId={}", userId, te.getId());
              } else {
                log.debug("No active timer for userId={}", userId);
              }
            });
  }

  /**
   * Получает все записи времени пользователя (исключая удаленные).
   *
   * @param userId ID пользователя
   * @return Flux записей времени
   */
  public Flux<TimeEntryDTO> getUserTimeEntries(UUID userId) {
    log.debug("Getting time entries for userId={}", userId);

    return timeEntryRepository
        .findByUserId(userId)
        .map(timeEntryMapper::toDTO)
        .doOnComplete(() -> log.debug("Time entries fetched for userId={}", userId));
  }

  /**
   * Получает записи времени для задачи (исключая удаленные).
   *
   * @param taskId ID задачи
   * @return Flux записей времени
   */
  public Flux<TimeEntryDTO> getTaskTimeEntries(UUID taskId) {
    log.debug("Getting time entries for taskId={}", taskId);

    return timeEntryRepository
        .findByTaskId(taskId)
        .map(timeEntryMapper::toDTO)
        .doOnComplete(() -> log.debug("Time entries fetched for taskId={}", taskId));
  }

  /**
   * Создает ручную запись времени (MANUAL).
   *
   * <p>Валидирует, что endTime > startTime. Рассчитывает duration и cost.
   *
   * @param dto DTO с данными записи
   * @param userId ID пользователя
   * @return Mono с созданной записью
   * @throws BusinessLogicException если endTime <= startTime
   */
  @Transactional
  public Mono<TimeEntryDTO> createManualEntry(TimeEntryDTO dto, UUID userId) {
    log.debug("Creating manual time entry for userId={}, taskId={}", userId, dto.getTaskId());

    if (dto.getEndTime() == null || !dto.getEndTime().isAfter(dto.getStartTime())) {
      return Mono.error(
          new BusinessLogicException("End time must be after start time for manual entries"));
    }

    return validateTask(dto.getTaskId())
        .then(
            dto.getHourlyRate() != null
                ? Mono.just(dto.getHourlyRate())
                : determineHourlyRate(dto.getTaskId(), userId))
        .flatMap(
            hourlyRate -> {
              TimeEntry timeEntry = timeEntryMapper.toEntity(dto);
              timeEntry.setUserId(userId);
              timeEntry.setEntryType(TimeEntryType.MANUAL);
              timeEntry.setHourlyRate(hourlyRate);

              // Рассчитываем duration
              Duration duration = Duration.between(dto.getStartTime(), dto.getEndTime());
              timeEntry.setDurationSeconds((int) duration.getSeconds());

              // Рассчитываем cost
              BigDecimal hours =
                  BigDecimal.valueOf(timeEntry.getDurationSeconds())
                      .divide(BigDecimal.valueOf(3600), 4, RoundingMode.HALF_UP);
              BigDecimal cost = hours.multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
              timeEntry.setCost(cost);

              timeEntry.setCreatedAt(LocalDateTime.now());
              timeEntry.setUpdatedAt(LocalDateTime.now());

              return timeEntryRepository
                  .save(timeEntry)
                  .map(timeEntryMapper::toDTO)
                  .doOnSuccess(
                      te ->
                          log.info(
                              "Manual time entry created: timeEntryId={}, userId={}, taskId={}",
                              te.getId(),
                              userId,
                              dto.getTaskId()));
            });
  }

  /**
   * Удаляет запись времени (soft delete).
   *
   * @param timeEntryId ID записи времени
   * @param userId ID пользователя, удаляющего запись
   * @param reason причина удаления
   * @return Mono пустой при успехе
   * @throws ResourceNotFoundException если запись не найдена
   */
  @Transactional
  public Mono<Void> deleteTimeEntry(UUID timeEntryId, UUID userId, String reason) {
    log.debug(
        "Soft deleting time entry: timeEntryId={}, deletedBy={}, reason='{}'",
        timeEntryId,
        userId,
        reason);

    return timeEntryRepository
        .findById(timeEntryId)
        .switchIfEmpty(
            Mono.error(
                new ResourceNotFoundException("Time entry not found with id: " + timeEntryId)))
        .flatMap(
            timeEntry -> {
              if (timeEntry.isActiveTimer()) {
                return Mono.error(
                    new BusinessLogicException("Cannot delete active timer. Stop it first."));
              }

              timeEntry.setDeletedAt(LocalDateTime.now());
              timeEntry.setDeletedBy(userId);
              timeEntry.setDeleteReason(reason);
              timeEntry.setUpdatedAt(LocalDateTime.now());

              return timeEntryRepository.save(timeEntry);
            })
        .then()
        .doOnSuccess(v -> log.info("Time entry soft deleted: timeEntryId={}", timeEntryId));
  }

  // ===== Private helper methods =====

  /**
   * Проверяет, что у пользователя нет активного таймера.
   *
   * @param userId ID пользователя
   * @return Mono пустой при успехе
   * @throws ConflictException если у пользователя есть активный таймер
   */
  private Mono<Void> checkNoActiveTimer(UUID userId) {
    return timeEntryRepository
        .hasActiveTimer(userId)
        .flatMap(
            hasTimer -> {
              if (hasTimer) {
                return Mono.error(
                    new ConflictException(
                        "User already has an active timer. Stop it before starting a new one."));
              }
              return Mono.empty();
            });
  }

  /**
   * Валидирует существование задачи.
   *
   * @param taskId ID задачи
   * @return Mono пустой при успехе
   * @throws ResourceNotFoundException если задача не найдена
   */
  private Mono<Void> validateTask(UUID taskId) {
    return taskRepository
        .findById(taskId)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException("Task not found with id: " + taskId)))
        .then();
  }

  /**
   * Определяет ставку биллинга для записи времени.
   *
   * <p>Приоритет: проект > пользователь.
   *
   * @param taskId ID задачи
   * @param userId ID пользователя
   * @return Mono со ставкой биллинга (может быть null)
   */
  private Mono<BigDecimal> determineHourlyRate(UUID taskId, UUID userId) {
    return taskRepository
        .findById(taskId)
        .flatMap(
            task ->
                projectRepository
                    .findById(task.getProjectId())
                    .flatMap(
                        project -> {
                          if (project.getHourlyRate() != null) {
                            log.debug(
                                "Using project hourly rate: {} for projectId={}",
                                project.getHourlyRate(),
                                project.getId());
                            return Mono.just(project.getHourlyRate());
                          }

                          // Fallback to user's hourly rate
                          return userRepository
                              .findById(userId)
                              .map(
                                  user -> {
                                    log.debug(
                                        "Using user hourly rate: {} for userId={}",
                                        user.getHourlyRate(),
                                        userId);
                                    return user.getHourlyRate();
                                  })
                              .defaultIfEmpty((BigDecimal) null);
                        }));
  }
}
