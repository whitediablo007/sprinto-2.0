package ru.get.tms.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.timeentry.TimeEntry;

/**
 * R2DBC репозиторий для работы с сущностью {@link TimeEntry}.
 *
 * <p>Предоставляет реактивные методы для CRUD операций и специализированных запросов к таблице
 * time_entries. Поддерживает работу с активными таймерами, биллингом и soft delete.
 */
public interface TimeEntryRepository extends ReactiveCrudRepository<TimeEntry, UUID> {

  /**
   * Находит все записи времени для указанной задачи (исключая удаленные).
   *
   * @param taskId идентификатор задачи
   * @return Flux записей времени задачи
   */
  @Query(
      "SELECT * FROM time_entries WHERE task_id = :taskId AND deleted_at IS NULL ORDER BY"
          + " start_time DESC")
  Flux<TimeEntry> findByTaskId(UUID taskId);

  /**
   * Находит все записи времени пользователя (исключая удаленные).
   *
   * @param userId идентификатор пользователя
   * @return Flux записей времени пользователя
   */
  @Query(
      "SELECT * FROM time_entries WHERE user_id = :userId AND deleted_at IS NULL ORDER BY"
          + " start_time DESC")
  Flux<TimeEntry> findByUserId(UUID userId);

  /**
   * Находит записи времени пользователя за указанный период (исключая удаленные).
   *
   * @param userId идентификатор пользователя
   * @param from начало периода
   * @param to конец периода
   * @return Flux записей времени за период
   */
  @Query(
      "SELECT * FROM time_entries WHERE user_id = :userId AND start_time >= :from AND start_time"
          + " <= :to AND deleted_at IS NULL ORDER BY start_time ASC")
  Flux<TimeEntry> findByUserIdAndPeriod(UUID userId, LocalDateTime from, LocalDateTime to);

  /**
   * Находит записи времени для задачи за указанный период (исключая удаленные).
   *
   * @param taskId идентификатор задачи
   * @param from начало периода
   * @param to конец периода
   * @return Flux записей времени за период
   */
  @Query(
      "SELECT * FROM time_entries WHERE task_id = :taskId AND start_time >= :from AND start_time"
          + " <= :to AND deleted_at IS NULL ORDER BY start_time ASC")
  Flux<TimeEntry> findByTaskIdAndPeriod(UUID taskId, LocalDateTime from, LocalDateTime to);

  /**
   * Находит активный таймер пользователя (запись с end_time IS NULL).
   *
   * <p>У пользователя может быть только один активный таймер.
   *
   * @param userId идентификатор пользователя
   * @return Mono активной записи времени или пустой Mono
   */
  @Query(
      "SELECT * FROM time_entries WHERE user_id = :userId AND end_time IS NULL AND deleted_at IS"
          + " NULL")
  Mono<TimeEntry> findActiveTimerByUserId(UUID userId);

  /**
   * Проверяет наличие активного таймера у пользователя.
   *
   * @param userId идентификатор пользователя
   * @return Mono с результатом проверки
   */
  @Query(
      "SELECT EXISTS(SELECT 1 FROM time_entries WHERE user_id = :userId AND end_time IS NULL AND"
          + " deleted_at IS NULL)")
  Mono<Boolean> hasActiveTimer(UUID userId);

  /**
   * Находит записи времени с пересечением указанного временного интервала для пользователя.
   *
   * <p>Используется для проверки конфликтов записей времени (FR-054, FR-064).
   *
   * @param userId идентификатор пользователя
   * @param startTime начало интервала
   * @param endTime конец интервала
   * @return Flux пересекающихся записей времени
   */
  @Query(
      "SELECT * FROM time_entries WHERE user_id = :userId AND deleted_at IS NULL AND ((start_time"
          + " <= :startTime AND end_time > :startTime) OR (start_time < :endTime AND end_time >="
          + " :endTime) OR (start_time >= :startTime AND end_time <= :endTime)) ORDER BY"
          + " start_time ASC")
  Flux<TimeEntry> findOverlappingEntries(
      UUID userId, LocalDateTime startTime, LocalDateTime endTime);

  /**
   * Подсчитывает общее затраченное время на задачу в секундах (исключая удаленные и активные
   * таймеры).
   *
   * @param taskId идентификатор задачи
   * @return Mono с общим временем в секундах (может быть null)
   */
  @Query(
      "SELECT COALESCE(SUM(duration_seconds), 0) FROM time_entries WHERE task_id = :taskId AND"
          + " end_time IS NOT NULL AND deleted_at IS NULL")
  Mono<Long> sumDurationSecondsByTaskId(UUID taskId);

  /**
   * Подсчитывает общую стоимость работ по задаче (исключая удаленные и активные таймеры).
   *
   * @param taskId идентификатор задачи
   * @return Mono с общей стоимостью (может быть null)
   */
  @Query(
      "SELECT COALESCE(SUM(cost), 0) FROM time_entries WHERE task_id = :taskId AND end_time IS"
          + " NOT NULL AND deleted_at IS NULL")
  Mono<Double> sumCostByTaskId(UUID taskId);

  /**
   * Находит все удаленные записи времени (для аудита).
   *
   * @return Flux удаленных записей времени
   */
  @Query("SELECT * FROM time_entries WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC")
  Flux<TimeEntry> findDeleted();

  /**
   * Находит удаленные записи времени пользователя (для восстановления админом).
   *
   * @param userId идентификатор пользователя
   * @return Flux удаленных записей времени пользователя
   */
  @Query(
      "SELECT * FROM time_entries WHERE user_id = :userId AND deleted_at IS NOT NULL ORDER BY"
          + " deleted_at DESC")
  Flux<TimeEntry> findDeletedByUserId(UUID userId);
}
