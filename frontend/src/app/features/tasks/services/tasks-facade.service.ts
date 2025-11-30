import { Injectable, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { Task, TaskCreateRequest, TaskUpdateStatusRequest, TaskStatus } from '../../../shared/models/task.model';
import * as TasksActions from '../store/tasks.actions';
import * as TasksSelectors from '../store/tasks.selectors';

/**
 * Tasks Facade Service.
 *
 * Предоставляет высокоуровневый API для работы с задачами,
 * скрывая детали реализации NgRx store от компонентов.
 *
 * Компоненты должны использовать Facade вместо прямого обращения к store.
 */
@Injectable({
  providedIn: 'root',
})
export class TasksFacadeService {
  private store = inject(Store);

  // Observables для компонентов
  tasks$ = this.store.select(TasksSelectors.selectAllTasks);
  selectedTask$ = this.store.select(TasksSelectors.selectSelectedTask);
  loading$ = this.store.select(TasksSelectors.selectTasksLoading);
  error$ = this.store.select(TasksSelectors.selectTasksError);
  tasksViewModel$ = this.store.select(TasksSelectors.selectTasksViewModel);

  // Statistics
  tasksCount$ = this.store.select(TasksSelectors.selectTasksCount);
  completedTasksCount$ = this.store.select(TasksSelectors.selectCompletedTasksCount);
  inProgressTasksCount$ = this.store.select(TasksSelectors.selectInProgressTasksCount);
  completionRate$ = this.store.select(TasksSelectors.selectTasksCompletionRate);

  /**
   * Загружает список задач с опциональными фильтрами.
   *
   * @param projectId опциональный ID проекта для фильтрации
   * @param assigneeId опциональный ID assignee для фильтрации
   */
  loadTasks(projectId?: string, assigneeId?: string): void {
    this.store.dispatch(TasksActions.loadTasks({ projectId, assigneeId }));
  }

  /**
   * Загружает одну задачу по ID.
   *
   * @param id ID задачи
   */
  loadTask(id: string): void {
    this.store.dispatch(TasksActions.loadTask({ id }));
  }

  /**
   * Создает новую задачу.
   *
   * @param taskData данные для создания задачи
   */
  createTask(taskData: TaskCreateRequest): void {
    this.store.dispatch(TasksActions.createTask({ taskData }));
  }

  /**
   * Обновляет существующую задачу.
   *
   * @param id ID задачи
   * @param taskData обновленные данные
   */
  updateTask(id: string, taskData: Partial<TaskCreateRequest>): void {
    this.store.dispatch(TasksActions.updateTask({ id, taskData }));
  }

  /**
   * Обновляет статус задачи.
   *
   * @param id ID задачи
   * @param status новый статус
   */
  updateTaskStatus(id: string, status: TaskStatus): void {
    this.store.dispatch(TasksActions.updateTaskStatus({ id, statusUpdate: { status } }));
  }

  /**
   * Удаляет задачу.
   *
   * @param id ID задачи
   */
  deleteTask(id: string): void {
    this.store.dispatch(TasksActions.deleteTask({ id }));
  }

  /**
   * Загружает подзадачи для родительской задачи.
   *
   * @param parentTaskId ID родительской задачи
   */
  loadSubtasks(parentTaskId: string): void {
    this.store.dispatch(TasksActions.loadSubtasks({ parentTaskId }));
  }

  /**
   * Выбирает задачу для отображения в деталях.
   *
   * @param id ID задачи или null для сброса
   */
  selectTask(id: string | null): void {
    this.store.dispatch(TasksActions.selectTask({ id }));
  }

  /**
   * Очищает ошибки в store.
   */
  clearError(): void {
    this.store.dispatch(TasksActions.clearTasksError());
  }

  /**
   * Получает задачу по ID как Observable.
   *
   * @param id ID задачи
   * @returns Observable<Task | null>
   */
  getTaskById(id: string): Observable<Task | null> {
    return this.store.select(TasksSelectors.selectTaskById(id));
  }

  /**
   * Получает задачи проекта как Observable.
   *
   * @param projectId ID проекта
   * @returns Observable<Task[]>
   */
  getTasksByProjectId(projectId: string): Observable<Task[]> {
    return this.store.select(TasksSelectors.selectTasksByProjectId(projectId));
  }

  /**
   * Получает задачи assignee как Observable.
   *
   * @param assigneeId ID пользователя
   * @returns Observable<Task[]>
   */
  getTasksByAssigneeId(assigneeId: string): Observable<Task[]> {
    return this.store.select(TasksSelectors.selectTasksByAssigneeId(assigneeId));
  }

  /**
   * Получает задачи по статусу как Observable.
   *
   * @param status статус задачи
   * @returns Observable<Task[]>
   */
  getTasksByStatus(status: TaskStatus): Observable<Task[]> {
    return this.store.select(TasksSelectors.selectTasksByStatus(status));
  }

  /**
   * Получает подзадачи как Observable.
   *
   * @param parentTaskId ID родительской задачи
   * @returns Observable<Task[]>
   */
  getSubtasks(parentTaskId: string): Observable<Task[]> {
    return this.store.select(TasksSelectors.selectSubtasks(parentTaskId));
  }

  /**
   * Получает корневые задачи проекта (без родителя) как Observable.
   *
   * @param projectId ID проекта
   * @returns Observable<Task[]>
   */
  getRootTasksByProjectId(projectId: string): Observable<Task[]> {
    return this.store.select(TasksSelectors.selectRootTasksByProjectId(projectId));
  }
}

