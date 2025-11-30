import { Injectable, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';
import { TimeEntry, TimeEntryCreateRequest, TimerUpdate } from '../../../shared/models/time-entry.model';
import * as TimeTrackingActions from '../store/time-tracking.actions';
import * as TimeTrackingSelectors from '../store/time-tracking.selectors';

/**
 * Timer Facade Service.
 *
 * Предоставляет высокоуровневый API для работы с таймерами и записями времени,
 * скрывая детали реализации NgRx store от компонентов.
 *
 * Компоненты должны использовать Facade вместо прямого обращения к store.
 */
@Injectable({
  providedIn: 'root',
})
export class TimerFacadeService {
  private store = inject(Store);

  // Observables для компонентов
  activeTimer$ = this.store.select(TimeTrackingSelectors.selectActiveTimer);
  timerUpdate$ = this.store.select(TimeTrackingSelectors.selectTimerUpdate);
  timeEntries$ = this.store.select(TimeTrackingSelectors.selectAllTimeEntries);
  loading$ = this.store.select(TimeTrackingSelectors.selectTimeTrackingLoading);
  error$ = this.store.select(TimeTrackingSelectors.selectTimeTrackingError);
  timerViewModel$ = this.store.select(TimeTrackingSelectors.selectTimerViewModel);
  timeEntriesViewModel$ = this.store.select(TimeTrackingSelectors.selectTimeEntriesViewModel);

  // Timer status
  hasActiveTimer$ = this.store.select(TimeTrackingSelectors.selectHasActiveTimer);
  timerElapsedSeconds$ = this.store.select(TimeTrackingSelectors.selectTimerElapsedSeconds);
  timerTaskTitle$ = this.store.select(TimeTrackingSelectors.selectTimerTaskTitle);

  // Statistics
  totalTrackedSeconds$ = this.store.select(TimeTrackingSelectors.selectTotalTrackedSeconds);
  totalCost$ = this.store.select(TimeTrackingSelectors.selectTotalCost);

  /**
   * Запускает таймер на задаче.
   *
   * @param taskId ID задачи
   */
  startTimer(taskId: string): void {
    this.store.dispatch(
      TimeTrackingActions.startTimer({
        request: { taskId },
      })
    );
  }

  /**
   * Останавливает активный таймер.
   */
  stopTimer(): void {
    this.store.dispatch(TimeTrackingActions.stopTimer());
  }

  /**
   * Загружает активный таймер текущего пользователя.
   */
  loadActiveTimer(): void {
    this.store.dispatch(TimeTrackingActions.loadActiveTimer());
  }

  /**
   * Обрабатывает WebSocket обновление таймера.
   *
   * @param update обновление от WebSocket
   */
  handleTimerUpdate(update: TimerUpdate): void {
    this.store.dispatch(TimeTrackingActions.timerUpdate({ update }));
  }

  /**
   * Загружает записи времени с опциональными фильтрами.
   *
   * @param taskId опциональный ID задачи для фильтрации
   * @param userId опциональный ID пользователя для фильтрации
   */
  loadTimeEntries(taskId?: string, userId?: string): void {
    this.store.dispatch(TimeTrackingActions.loadTimeEntries({ taskId, userId }));
  }

  /**
   * Создает ручную запись времени.
   *
   * @param entryData данные ручной записи
   */
  createManualTimeEntry(entryData: TimeEntryCreateRequest): void {
    this.store.dispatch(TimeTrackingActions.createManualTimeEntry({ entryData }));
  }

  /**
   * Удаляет запись времени (soft delete).
   *
   * @param id ID записи времени
   * @param reason опциональная причина удаления
   */
  deleteTimeEntry(id: string, reason?: string): void {
    this.store.dispatch(TimeTrackingActions.deleteTimeEntry({ id, reason }));
  }

  /**
   * Очищает ошибки в store.
   */
  clearError(): void {
    this.store.dispatch(TimeTrackingActions.clearTimeTrackingError());
  }

  /**
   * Получает записи времени для задачи как Observable.
   *
   * @param taskId ID задачи
   * @returns Observable<TimeEntry[]>
   */
  getTimeEntriesByTaskId(taskId: string): Observable<TimeEntry[]> {
    return this.store.select(TimeTrackingSelectors.selectTimeEntriesByTaskId(taskId));
  }

  /**
   * Получает записи времени пользователя как Observable.
   *
   * @param userId ID пользователя
   * @returns Observable<TimeEntry[]>
   */
  getTimeEntriesByUserId(userId: string): Observable<TimeEntry[]> {
    return this.store.select(TimeTrackingSelectors.selectTimeEntriesByUserId(userId));
  }

  /**
   * Получает активные записи времени (не завершенные).
   *
   * @returns Observable<TimeEntry[]>
   */
  getActiveTimeEntries(): Observable<TimeEntry[]> {
    return this.store.select(TimeTrackingSelectors.selectActiveTimeEntries);
  }

  /**
   * Получает завершенные записи времени.
   *
   * @returns Observable<TimeEntry[]>
   */
  getCompletedTimeEntries(): Observable<TimeEntry[]> {
    return this.store.select(TimeTrackingSelectors.selectCompletedTimeEntries);
  }

  /**
   * Получает общее время для задачи.
   *
   * @param taskId ID задачи
   * @returns Observable<number> количество секунд
   */
  getTaskTotalTime(taskId: string): Observable<number> {
    return this.store.select(TimeTrackingSelectors.selectTaskTotalTime(taskId));
  }
}

