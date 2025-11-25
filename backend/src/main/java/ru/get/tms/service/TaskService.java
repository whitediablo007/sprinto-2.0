package ru.get.tms.service;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.task.Task;
import ru.get.tms.domain.task.TaskStatus;
import ru.get.tms.dto.task.TaskCreateDTO;
import ru.get.tms.dto.task.TaskResponseDTO;
import ru.get.tms.exception.BusinessLogicException;
import ru.get.tms.exception.ResourceNotFoundException;
import ru.get.tms.mapper.TaskMapper;
import ru.get.tms.repository.ProjectRepository;
import ru.get.tms.repository.TaskRepository;

/**
 * Service для управления задачами.
 *
 * <p>Реализует бизнес-логику создания, обновления и управления жизненным циклом задач. Поддерживает
 * иерархию задач до 5 уровней.
 *
 * <p>Следует реактивному паттерну обработки ошибок (NFR-027).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

  private static final int MAX_HIERARCHY_LEVEL = 4;

  private final TaskRepository taskRepository;
  private final ProjectRepository projectRepository;
  private final TaskMapper taskMapper;

  /**
   * Создает новую задачу в проекте.
   *
   * @param dto DTO с данными задачи
   * @param createdBy ID пользователя-создателя
   * @return Mono с созданной задачей
   * @throws ResourceNotFoundException если проект не найден
   * @throws BusinessLogicException если превышена максимальная глубина иерархии
   */
  @Transactional
  public Mono<TaskResponseDTO> create(TaskCreateDTO dto, UUID createdBy) {
    log.debug(
        "Creating new task: title='{}', projectId={}, createdBy={}",
        dto.getTitle(),
        dto.getProjectId(),
        createdBy);

    return validateProject(dto.getProjectId())
        .then(validateHierarchy(dto.getParentTaskId()))
        .flatMap(
            parentLevel -> {
              Task task = taskMapper.toEntity(dto);
              task.setCreatedBy(createdBy);
              task.setCreatedAt(LocalDateTime.now());
              task.setUpdatedAt(LocalDateTime.now());

              if (dto.getParentTaskId() != null) {
                task.setHierarchyLevel(parentLevel + 1);
              } else {
                task.setHierarchyLevel(0);
              }

              return taskRepository
                  .save(task)
                  .flatMap(this::enrichResponseDTO)
                  .doOnSuccess(
                      t ->
                          log.info(
                              "Task created successfully: id={}, title='{}'",
                              t.getId(),
                              t.getTitle()));
            });
  }

  /**
   * Находит задачу по ID.
   *
   * @param taskId ID задачи
   * @return Mono с задачей или пустой Mono
   * @throws ResourceNotFoundException если задача не найдена
   */
  public Mono<TaskResponseDTO> findById(UUID taskId) {
    log.debug("Finding task by id: {}", taskId);

    return taskRepository
        .findById(taskId)
        .flatMap(this::enrichResponseDTO)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException("Task not found with id: " + taskId)));
  }

  /**
   * Находит все задачи проекта.
   *
   * @param projectId ID проекта
   * @return Flux задач проекта
   */
  public Flux<TaskResponseDTO> findByProjectId(UUID projectId) {
    log.debug("Finding tasks by projectId: {}", projectId);

    return taskRepository
        .findByProjectId(projectId)
        .flatMap(this::enrichResponseDTO)
        .doOnComplete(() -> log.debug("Tasks fetched for projectId: {}", projectId));
  }

  /**
   * Находит все задачи, назначенные пользователю.
   *
   * @param assigneeId ID исполнителя
   * @return Flux задач исполнителя
   */
  public Flux<TaskResponseDTO> findByAssigneeId(UUID assigneeId) {
    log.debug("Finding tasks by assigneeId: {}", assigneeId);

    return taskRepository
        .findByAssigneeId(assigneeId)
        .flatMap(this::enrichResponseDTO)
        .doOnComplete(() -> log.debug("Tasks fetched for assigneeId: {}", assigneeId));
  }

  /**
   * Находит подзадачи указанной родительской задачи.
   *
   * @param parentTaskId ID родительской задачи
   * @return Flux подзадач
   */
  public Flux<TaskResponseDTO> findSubtasks(UUID parentTaskId) {
    log.debug("Finding subtasks of parentTaskId: {}", parentTaskId);

    return taskRepository
        .findByParentTaskId(parentTaskId)
        .flatMap(this::enrichResponseDTO)
        .doOnComplete(() -> log.debug("Subtasks fetched for parentTaskId: {}", parentTaskId));
  }

  /**
   * Обновляет статус задачи.
   *
   * <p>При переходе в COMPLETED устанавливается completedAt.
   *
   * @param taskId ID задачи
   * @param newStatus новый статус
   * @return Mono с обновленной задачей
   * @throws ResourceNotFoundException если задача не найдена
   * @throws BusinessLogicException если переход статуса недопустим
   */
  @Transactional
  public Mono<TaskResponseDTO> updateStatus(UUID taskId, TaskStatus newStatus) {
    log.debug("Updating task status: taskId={}, newStatus={}", taskId, newStatus);

    return taskRepository
        .findById(taskId)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException("Task not found with id: " + taskId)))
        .flatMap(
            task -> {
              validateStatusTransition(task.getStatus(), newStatus);

              task.setStatus(newStatus);
              task.setUpdatedAt(LocalDateTime.now());

              if (newStatus == TaskStatus.COMPLETED) {
                task.setCompletedAt(LocalDateTime.now());
              }

              return taskRepository.save(task);
            })
        .flatMap(this::enrichResponseDTO)
        .doOnSuccess(
            t -> log.info("Task status updated: taskId={}, status={}", t.getId(), t.getStatus()));
  }

  /**
   * Обновляет задачу.
   *
   * @param taskId ID задачи
   * @param dto DTO с обновленными данными
   * @return Mono с обновленной задачей
   * @throws ResourceNotFoundException если задача не найдена
   */
  @Transactional
  public Mono<TaskResponseDTO> update(UUID taskId, TaskCreateDTO dto) {
    log.debug("Updating task: taskId={}", taskId);

    return taskRepository
        .findById(taskId)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException("Task not found with id: " + taskId)))
        .flatMap(
            task -> {
              taskMapper.updateEntityFromDto(dto, task);
              task.setUpdatedAt(LocalDateTime.now());

              return taskRepository.save(task);
            })
        .flatMap(this::enrichResponseDTO)
        .doOnSuccess(t -> log.info("Task updated successfully: taskId={}", t.getId()));
  }

  /**
   * Удаляет задачу.
   *
   * @param taskId ID задачи
   * @return Mono пустой при успехе
   * @throws ResourceNotFoundException если задача не найдена
   */
  @Transactional
  public Mono<Void> delete(UUID taskId) {
    log.debug("Deleting task: taskId={}", taskId);

    return taskRepository
        .findById(taskId)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException("Task not found with id: " + taskId)))
        .flatMap(task -> taskRepository.delete(task))
        .doOnSuccess(v -> log.info("Task deleted: taskId={}", taskId));
  }

  // ===== Private helper methods =====

  /**
   * Валидирует существование проекта.
   *
   * @param projectId ID проекта
   * @return Mono пустой при успехе
   * @throws ResourceNotFoundException если проект не найден
   */
  private Mono<Void> validateProject(UUID projectId) {
    return projectRepository
        .findById(projectId)
        .switchIfEmpty(
            Mono.error(new ResourceNotFoundException("Project not found with id: " + projectId)))
        .then();
  }

  /**
   * Валидирует иерархию задач.
   *
   * <p>Проверяет, что не превышена максимальная глубина вложенности (5 уровней).
   *
   * @param parentTaskId ID родительской задачи (может быть null)
   * @return Mono с уровнем родительской задачи (или -1 если нет родителя)
   * @throws BusinessLogicException если превышена максимальная глубина
   */
  private Mono<Integer> validateHierarchy(UUID parentTaskId) {
    if (parentTaskId == null) {
      return Mono.just(-1);
    }

    return taskRepository
        .findById(parentTaskId)
        .switchIfEmpty(
            Mono.error(
                new ResourceNotFoundException("Parent task not found with id: " + parentTaskId)))
        .flatMap(
            parentTask -> {
              if (parentTask.getHierarchyLevel() >= MAX_HIERARCHY_LEVEL) {
                return Mono.error(
                    new BusinessLogicException("Maximum hierarchy level exceeded (max 5 levels)"));
              }
              return Mono.just(parentTask.getHierarchyLevel());
            });
  }

  /**
   * Валидирует переход между статусами.
   *
   * @param currentStatus текущий статус
   * @param newStatus новый статус
   * @throws BusinessLogicException если переход недопустим
   */
  private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
    if (currentStatus == newStatus) {
      return; // Нет изменений
    }

    boolean validTransition =
        switch (currentStatus) {
          case NEW -> newStatus == TaskStatus.IN_PROGRESS || newStatus == TaskStatus.CANCELLED;
          case IN_PROGRESS ->
              newStatus == TaskStatus.TESTING
                  || newStatus == TaskStatus.BLOCKED
                  || newStatus == TaskStatus.COMPLETED
                  || newStatus == TaskStatus.CANCELLED;
          case TESTING ->
              newStatus == TaskStatus.IN_PROGRESS
                  || newStatus == TaskStatus.COMPLETED
                  || newStatus == TaskStatus.CANCELLED;
          case BLOCKED -> newStatus == TaskStatus.IN_PROGRESS || newStatus == TaskStatus.CANCELLED;
          case COMPLETED, CANCELLED -> false; // Финальные статусы
        };

    if (!validTransition) {
      throw new BusinessLogicException(
          String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
    }
  }

  /**
   * Обогащает TaskResponseDTO дополнительными данными.
   *
   * <p>В базовой реализации просто маппит entity → DTO. В будущем можно добавить join-запросы для
   * enrichment полей (assigneeName, projectName и т.д.).
   *
   * @param task Task entity
   * @return Mono с обогащенным DTO
   */
  private Mono<TaskResponseDTO> enrichResponseDTO(Task task) {
    TaskResponseDTO dto = taskMapper.toResponseDTO(task);
    // TODO: добавить enrichment из связанных таблиц (users, projects) при необходимости
    return Mono.just(dto);
  }
}
