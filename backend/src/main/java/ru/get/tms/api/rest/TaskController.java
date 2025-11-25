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
import ru.get.tms.domain.task.TaskStatus;
import ru.get.tms.dto.task.TaskCreateDTO;
import ru.get.tms.dto.task.TaskResponseDTO;
import ru.get.tms.service.TaskService;

/**
 * REST контроллер для управления задачами.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>POST /api/tasks - создание новой задачи
 *   <li>GET /api/tasks/{id} - получение задачи по ID
 *   <li>PUT /api/tasks/{id} - обновление задачи
 *   <li>DELETE /api/tasks/{id} - удаление задачи
 *   <li>PUT /api/tasks/{id}/status - обновление статуса задачи
 *   <li>GET /api/tasks/project/{projectId} - получение всех задач проекта
 *   <li>GET /api/tasks/assigned - получение задач текущего пользователя
 *   <li>GET /api/tasks/{id}/subtasks - получение подзадач
 * </ul>
 *
 * <p>Реализует требования FR-001 (создание задач), FR-002 (управление статусами).
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final TaskService taskService;

  /**
   * Создает новую задачу в проекте.
   *
   * <p>Текущий пользователь автоматически устанавливается как создатель задачи.
   *
   * @param dto данные для создания задачи
   * @param authentication информация о текущем пользователе
   * @return Mono с созданной задачей
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<TaskResponseDTO> createTask(
      @Valid @RequestBody TaskCreateDTO dto, Authentication authentication) {
    log.debug("Creating task: title='{}', projectId={}", dto.getTitle(), dto.getProjectId());

    UUID currentUserId = extractUserId(authentication);
    return taskService.create(dto, currentUserId);
  }

  /**
   * Получает задачу по ID.
   *
   * @param id ID задачи
   * @return Mono с задачей
   */
  @GetMapping("/{id}")
  public Mono<TaskResponseDTO> getTask(@PathVariable UUID id) {
    log.debug("Getting task by id: {}", id);
    return taskService.findById(id);
  }

  /**
   * Обновляет задачу.
   *
   * @param id ID задачи
   * @param dto обновленные данные задачи
   * @return Mono с обновленной задачей
   */
  @PutMapping("/{id}")
  public Mono<TaskResponseDTO> updateTask(
      @PathVariable UUID id, @Valid @RequestBody TaskCreateDTO dto) {
    log.debug("Updating task: id={}", id);
    return taskService.update(id, dto);
  }

  /**
   * Удаляет задачу.
   *
   * @param id ID задачи
   * @return Mono пустой при успехе
   */
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteTask(@PathVariable UUID id) {
    log.debug("Deleting task: id={}", id);
    return taskService.delete(id);
  }

  /**
   * Обновляет статус задачи.
   *
   * <p>При переходе в COMPLETED автоматически устанавливается completedAt.
   *
   * @param id ID задачи
   * @param request запрос с новым статусом
   * @return Mono с обновленной задачей
   */
  @PutMapping("/{id}/status")
  public Mono<TaskResponseDTO> updateTaskStatus(
      @PathVariable UUID id, @Valid @RequestBody UpdateStatusRequest request) {
    log.debug("Updating task status: id={}, newStatus={}", id, request.getStatus());
    return taskService.updateStatus(id, request.getStatus());
  }

  /**
   * Получает все задачи проекта.
   *
   * @param projectId ID проекта
   * @return Flux задач проекта
   */
  @GetMapping("/project/{projectId}")
  public Flux<TaskResponseDTO> getProjectTasks(@PathVariable UUID projectId) {
    log.debug("Getting tasks for project: {}", projectId);
    return taskService.findByProjectId(projectId);
  }

  /**
   * Получает все задачи, назначенные текущему пользователю.
   *
   * @param authentication информация о текущем пользователе
   * @return Flux задач пользователя
   */
  @GetMapping("/assigned")
  public Flux<TaskResponseDTO> getAssignedTasks(Authentication authentication) {
    UUID currentUserId = extractUserId(authentication);
    log.debug("Getting assigned tasks for user: {}", currentUserId);
    return taskService.findByAssigneeId(currentUserId);
  }

  /**
   * Получает подзадачи указанной родительской задачи.
   *
   * @param id ID родительской задачи
   * @return Flux подзадач
   */
  @GetMapping("/{id}/subtasks")
  public Flux<TaskResponseDTO> getSubtasks(@PathVariable UUID id) {
    log.debug("Getting subtasks for task: {}", id);
    return taskService.findSubtasks(id);
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

  /** DTO для запроса обновления статуса задачи. */
  public static class UpdateStatusRequest {
    private TaskStatus status;

    public TaskStatus getStatus() {
      return status;
    }

    public void setStatus(TaskStatus status) {
      this.status = status;
    }
  }
}
