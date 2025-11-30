import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import * as TimeTrackingActions from './time-tracking.actions';
import { TimeTrackingApiService } from '../services/time-tracking-api.service';

/**
 * Time Tracking Effects.
 *
 * Обрабатывает side effects для операций с таймерами и записями времени:
 * - API вызовы для таймеров (start/stop/active)
 * - API вызовы для ручных записей времени
 * - Обработка ошибок
 *
 * Использует TimeTrackingApiService для HTTP запросов.
 */
@Injectable()
export class TimeTrackingEffects {
  private actions$ = inject(Actions);
  private timeTrackingApiService = inject(TimeTrackingApiService);

  /**
   * Start timer effect.
   */
  startTimer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TimeTrackingActions.startTimer),
      switchMap(({ request }) => {
        return this.timeTrackingApiService.startTimer(request).pipe(
          map((timeEntry) => TimeTrackingActions.startTimerSuccess({ timeEntry })),
          catchError((error) =>
            of(TimeTrackingActions.startTimerFailure({ error: error.message || 'Failed to start timer' }))
          )
        );
      })
    )
  );

  /**
   * Stop timer effect.
   */
  stopTimer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TimeTrackingActions.stopTimer),
      switchMap(() => {
        return this.timeTrackingApiService.stopTimer().pipe(
          map((timeEntry) => TimeTrackingActions.stopTimerSuccess({ timeEntry })),
          catchError((error) =>
            of(TimeTrackingActions.stopTimerFailure({ error: error.message || 'Failed to stop timer' }))
          )
        );
      })
    )
  );

  /**
   * Load active timer effect.
   */
  loadActiveTimer$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TimeTrackingActions.loadActiveTimer),
      switchMap(() => {
        return this.timeTrackingApiService.getActiveTimer().pipe(
          map((timeEntry) => TimeTrackingActions.loadActiveTimerSuccess({ timeEntry })),
          catchError((error) =>
            of(
              TimeTrackingActions.loadActiveTimerFailure({
                error: error.message || 'Failed to load active timer',
              })
            )
          )
        );
      })
    )
  );

  /**
   * Load time entries effect.
   */
  loadTimeEntries$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TimeTrackingActions.loadTimeEntries),
      switchMap(({ taskId, userId }) => {
        return this.timeTrackingApiService.getTimeEntries({ taskId, userId }).pipe(
          map((timeEntries) => TimeTrackingActions.loadTimeEntriesSuccess({ timeEntries })),
          catchError((error) =>
            of(
              TimeTrackingActions.loadTimeEntriesFailure({
                error: error.message || 'Failed to load time entries',
              })
            )
          )
        );
      })
    )
  );

  /**
   * Create manual time entry effect.
   */
  createManualTimeEntry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TimeTrackingActions.createManualTimeEntry),
      switchMap(({ entryData }) => {
        return this.timeTrackingApiService.createManualEntry(entryData).pipe(
          map((timeEntry) => TimeTrackingActions.createManualTimeEntrySuccess({ timeEntry })),
          catchError((error) =>
            of(
              TimeTrackingActions.createManualTimeEntryFailure({
                error: error.message || 'Failed to create manual entry',
              })
            )
          )
        );
      })
    )
  );

  /**
   * Delete time entry effect.
   */
  deleteTimeEntry$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TimeTrackingActions.deleteTimeEntry),
      switchMap(({ id, reason }) => {
        return this.timeTrackingApiService.deleteTimeEntry(id, reason).pipe(
          map(() => TimeTrackingActions.deleteTimeEntrySuccess({ id })),
          catchError((error) =>
            of(
              TimeTrackingActions.deleteTimeEntryFailure({
                error: error.message || 'Failed to delete time entry',
              })
            )
          )
        );
      })
    )
  );
}

