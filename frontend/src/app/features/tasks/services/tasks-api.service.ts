import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Task, TaskCreateRequest, TaskUpdateStatusRequest, TaskFilters } from '../../../shared/models/task.model';
import { environment } from '../../../../environments/environment';

/**
 * Tasks API Service.
 *
 * Отвечает за HTTP запросы к backend API для управления задачами.
 * Все endpoints соответствуют TaskController на backend.
 *
 * Базовый URL: /api/tasks
 */
@Injectable({
  providedIn: 'root',
})
export class TasksApiService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/tasks`;

  /**
   * Получает список задач с фильтрацией.
   *
   * @param filters опциональные фильтры (projectId, assigneeId, status)
   * @returns Observable<Task[]>
   */
  getTasks(filters?: TaskFilters): Observable<Task[]> {
    let params = new HttpParams();

    if (filters?.projectId) {
      // GET /api/tasks/project/{projectId}
      return this.http.get<Task[]>(`${this.apiUrl}/project/${filters.projectId}`);
    }

    if (filters?.assigneeId) {
      // GET /api/tasks/assigned (implicitly uses current user from JWT)
      return this.http.get<Task[]>(`${this.apiUrl}/assigned`);
    }

    // По умолчанию загружаем задачи текущего пользователя
    return this.http.get<Task[]>(`${this.apiUrl}/assigned`);
  }

  /**
   * Получает задачу по ID.
   *
   * GET /api/tasks/{id}
   *
   * @param id ID задачи
   * @returns Observable<Task>
   */
  getTaskById(id: string): Observable<Task> {
    return this.http.get<Task>(`${this.apiUrl}/${id}`);
  }

  /**
   * Создает новую задачу.
   *
   * POST /api/tasks
   *
   * @param taskData данные для создания задачи
   * @returns Observable<Task>
   */
  createTask(taskData: TaskCreateRequest): Observable<Task> {
    return this.http.post<Task>(this.apiUrl, taskData);
  }

  /**
   * Обновляет существующую задачу.
   *
   * PUT /api/tasks/{id}
   *
   * @param id ID задачи
   * @param taskData обновленные данные
   * @returns Observable<Task>
   */
  updateTask(id: string, taskData: Partial<TaskCreateRequest>): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}`, taskData);
  }

  /**
   * Обновляет статус задачи.
   *
   * PUT /api/tasks/{id}/status
   *
   * @param id ID задачи
   * @param statusUpdate новый статус
   * @returns Observable<Task>
   */
  updateTaskStatus(id: string, statusUpdate: TaskUpdateStatusRequest): Observable<Task> {
    return this.http.put<Task>(`${this.apiUrl}/${id}/status`, statusUpdate);
  }

  /**
   * Удаляет задачу.
   *
   * DELETE /api/tasks/{id}
   *
   * @param id ID задачи
   * @returns Observable<void>
   */
  deleteTask(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Получает подзадачи для родительской задачи.
   *
   * GET /api/tasks/{id}/subtasks
   *
   * @param parentTaskId ID родительской задачи
   * @returns Observable<Task[]>
   */
  getSubtasks(parentTaskId: string): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.apiUrl}/${parentTaskId}/subtasks`);
  }

  /**
   * Получает задачи проекта.
   *
   * GET /api/tasks/project/{projectId}
   *
   * @param projectId ID проекта
   * @returns Observable<Task[]>
   */
  getProjectTasks(projectId: string): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.apiUrl}/project/${projectId}`);
  }

  /**
   * Получает задачи текущего пользователя (назначенные на него).
   *
   * GET /api/tasks/assigned
   *
   * @returns Observable<Task[]>
   */
  getMyTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.apiUrl}/assigned`);
  }
}

