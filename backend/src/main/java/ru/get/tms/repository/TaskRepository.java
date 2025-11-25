package ru.get.tms.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.task.Task;
import ru.get.tms.domain.task.TaskStatus;

/**
 * R2DBC репозиторий для работы с сущностью {@link Task}.
 *
 * <p>Предоставляет реактивные методы для CRUD операций и специализированных запросов к таблице
 * tasks. Поддерживает работу с иерархией задач и различными фильтрами.
 */
public interface TaskRepository extends ReactiveCrudRepository<Task, UUID> {

  /**
   * Находит все задачи проекта.
   *
   * @param projectId идентификатор проекта
   * @return Flux задач проекта
   */
  @Query("SELECT * FROM tasks WHERE project_id = :projectId ORDER BY created_at DESC")
  Flux<Task> findByProjectId(UUID projectId);

  /**
   * Находит все задачи, назначенные указанному пользователю.
   *
   * @param assigneeId идентификатор исполнителя
   * @return Flux задач исполнителя
   */
  @Query("SELECT * FROM tasks WHERE assignee_id = :assigneeId ORDER BY deadline ASC NULLS LAST")
  Flux<Task> findByAssigneeId(UUID assigneeId);

  /**
   * Находит все задачи проекта с указанным статусом.
   *
   * @param projectId идентификатор проекта
   * @param status статус задач
   * @return Flux задач проекта с заданным статусом
   */
  @Query(
      "SELECT * FROM tasks WHERE project_id = :projectId AND status = :status ORDER BY created_at"
          + " DESC")
  Flux<Task> findByProjectIdAndStatus(UUID projectId, TaskStatus status);

  /**
   * Находит все задачи исполнителя с указанным статусом.
   *
   * @param assigneeId идентификатор исполнителя
   * @param status статус задач
   * @return Flux задач исполнителя с заданным статусом
   */
  @Query(
      "SELECT * FROM tasks WHERE assignee_id = :assigneeId AND status = :status ORDER BY deadline"
          + " ASC NULLS LAST")
  Flux<Task> findByAssigneeIdAndStatus(UUID assigneeId, TaskStatus status);

  /**
   * Находит все подзадачи указанной родительской задачи.
   *
   * @param parentTaskId идентификатор родительской задачи
   * @return Flux подзадач
   */
  @Query("SELECT * FROM tasks WHERE parent_task_id = :parentTaskId ORDER BY created_at ASC")
  Flux<Task> findByParentTaskId(UUID parentTaskId);

  /**
   * Находит все задачи с дедлайном до указанной даты.
   *
   * @param deadline граничная дата дедлайна
   * @return Flux задач с дедлайном до указанной даты
   */
  @Query(
      "SELECT * FROM tasks WHERE deadline IS NOT NULL AND deadline <= :deadline AND status NOT IN"
          + " ('COMPLETED', 'CANCELLED') ORDER BY deadline ASC")
  Flux<Task> findByDeadlineBefore(LocalDateTime deadline);

  /**
   * Находит задачи с приближающимся дедлайном (в течение указанного периода).
   *
   * @param from начало периода
   * @param to конец периода
   * @return Flux задач с дедлайном в указанном периоде
   */
  @Query(
      "SELECT * FROM tasks WHERE deadline IS NOT NULL AND deadline >= :from AND deadline <= :to"
          + " AND status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY deadline ASC")
  Flux<Task> findByDeadlineBetween(LocalDateTime from, LocalDateTime to);

  /**
   * Подсчитывает количество задач проекта.
   *
   * @param projectId идентификатор проекта
   * @return Mono с количеством задач
   */
  @Query("SELECT COUNT(*) FROM tasks WHERE project_id = :projectId")
  Mono<Long> countByProjectId(UUID projectId);

  /**
   * Подсчитывает количество задач проекта с указанным статусом.
   *
   * @param projectId идентификатор проекта
   * @param status статус задач
   * @return Mono с количеством задач
   */
  @Query("SELECT COUNT(*) FROM tasks WHERE project_id = :projectId AND status = :status")
  Mono<Long> countByProjectIdAndStatus(UUID projectId, TaskStatus status);

  /**
   * Подсчитывает количество завершенных подзадач у родительской задачи.
   *
   * @param parentTaskId идентификатор родительской задачи
   * @return Mono с количеством завершенных подзадач
   */
  @Query(
      "SELECT COUNT(*) FROM tasks WHERE parent_task_id = :parentTaskId AND status IN ('COMPLETED',"
          + " 'CANCELLED')")
  Mono<Long> countCompletedSubtasks(UUID parentTaskId);

  /**
   * Находит все задачи, созданные указанным пользователем.
   *
   * @param createdBy идентификатор создателя задач
   * @return Flux задач, созданных пользователем
   */
  @Query("SELECT * FROM tasks WHERE created_by = :createdBy ORDER BY created_at DESC")
  Flux<Task> findByCreatedBy(UUID createdBy);
}
