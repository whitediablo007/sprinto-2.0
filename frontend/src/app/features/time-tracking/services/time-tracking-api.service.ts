import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  TimeEntry,
  TimeEntryCreateRequest,
  StartTimerRequest,
  DeleteTimeEntryRequest,
} from '../../../shared/models/time-entry.model';
import { environment } from '../../../../environments/environment';

/**
 * Time Tracking API Service.
 *
 * Отвечает за HTTP запросы к backend API для управления таймерами и записями времени.
 * Все endpoints соответствуют TimeTrackingController на backend.
 *
 * Базовый URL: /api/time-entries
 */
@Injectable({
  providedIn: 'root',
})
export class TimeTrackingApiService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/time-entries`;

  /**
   * Запускает таймер на задаче.
   *
   * POST /api/time-entries/timer/start
   *
   * @param request запрос с taskId
   * @returns Observable<TimeEntry> созданная запись времени (активный таймер)
   */
  startTimer(request: StartTimerRequest): Observable<TimeEntry> {
    return this.http.post<TimeEntry>(`${this.apiUrl}/timer/start`, request);
  }

  /**
   * Останавливает активный таймер текущего пользователя.
   *
   * POST /api/time-entries/timer/stop
   *
   * @returns Observable<TimeEntry> остановленная запись времени
   */
  stopTimer(): Observable<TimeEntry> {
    return this.http.post<TimeEntry>(`${this.apiUrl}/timer/stop`, {});
  }

  /**
   * Получает активный таймер текущего пользователя.
   *
   * GET /api/time-entries/timer/active
   *
   * @returns Observable<TimeEntry | null>
   */
  getActiveTimer(): Observable<TimeEntry | null> {
    return this.http.get<TimeEntry | null>(`${this.apiUrl}/timer/active`);
  }

  /**
   * Создает ручную запись времени.
   *
   * POST /api/time-entries
   *
   * @param entryData данные ручной записи
   * @returns Observable<TimeEntry>
   */
  createManualEntry(entryData: TimeEntryCreateRequest): Observable<TimeEntry> {
    return this.http.post<TimeEntry>(this.apiUrl, entryData);
  }

  /**
   * Получает все записи времени текущего пользователя.
   *
   * GET /api/time-entries/my
   *
   * @returns Observable<TimeEntry[]>
   */
  getMyTimeEntries(): Observable<TimeEntry[]> {
    return this.http.get<TimeEntry[]>(`${this.apiUrl}/my`);
  }

  /**
   * Получает записи времени для задачи.
   *
   * GET /api/time-entries/task/{taskId}
   *
   * @param taskId ID задачи
   * @returns Observable<TimeEntry[]>
   */
  getTaskTimeEntries(taskId: string): Observable<TimeEntry[]> {
    return this.http.get<TimeEntry[]>(`${this.apiUrl}/task/${taskId}`);
  }

  /**
   * Удаляет запись времени (soft delete).
   *
   * DELETE /api/time-entries/{id}
   *
   * @param id ID записи времени
   * @param reason опциональная причина удаления
   * @returns Observable<void>
   */
  deleteTimeEntry(id: string, reason?: string): Observable<void> {
    const body: DeleteTimeEntryRequest = reason ? { reason } : {};
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { body });
  }

  /**
   * Получает записи времени с фильтрацией.
   *
   * @param options опции для фильтрации (taskId или userId)
   * @returns Observable<TimeEntry[]>
   */
  getTimeEntries(options?: { taskId?: string; userId?: string }): Observable<TimeEntry[]> {
    if (options?.taskId) {
      return this.getTaskTimeEntries(options.taskId);
    }

    // По умолчанию загружаем записи текущего пользователя
    return this.getMyTimeEntries();
  }
}

