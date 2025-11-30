import { createAction, props } from '@ngrx/store';
import {
  TimeEntry,
  TimeEntryCreateRequest,
  TimerUpdate,
  StartTimerRequest,
} from '../../../shared/models/time-entry.model';

/**
 * Time Tracking Actions для NgRx Store.
 *
 * Определяет все действия для управления таймерами и записями времени:
 * - Управление таймерами (start, stop, active)
 * - CRUD операций для ручных записей времени
 * - WebSocket обновления таймера
 * - Загрузка истории времени
 */

// Timer Actions
export const startTimer = createAction('[Time Tracking] Start Timer', props<{ request: StartTimerRequest }>());

export const startTimerSuccess = createAction(
  '[Time Tracking] Start Timer Success',
  props<{ timeEntry: TimeEntry }>()
);

export const startTimerFailure = createAction(
  '[Time Tracking] Start Timer Failure',
  props<{ error: string }>()
);

export const stopTimer = createAction('[Time Tracking] Stop Timer');

export const stopTimerSuccess = createAction(
  '[Time Tracking] Stop Timer Success',
  props<{ timeEntry: TimeEntry }>()
);

export const stopTimerFailure = createAction('[Time Tracking] Stop Timer Failure', props<{ error: string }>());

// Load Active Timer
export const loadActiveTimer = createAction('[Time Tracking] Load Active Timer');

export const loadActiveTimerSuccess = createAction(
  '[Time Tracking] Load Active Timer Success',
  props<{ timeEntry: TimeEntry | null }>()
);

export const loadActiveTimerFailure = createAction(
  '[Time Tracking] Load Active Timer Failure',
  props<{ error: string }>()
);

// WebSocket Timer Updates
export const timerUpdate = createAction('[Time Tracking] Timer Update', props<{ update: TimerUpdate }>());

// Load Time Entries
export const loadTimeEntries = createAction(
  '[Time Tracking] Load Time Entries',
  props<{ taskId?: string; userId?: string }>()
);

export const loadTimeEntriesSuccess = createAction(
  '[Time Tracking] Load Time Entries Success',
  props<{ timeEntries: TimeEntry[] }>()
);

export const loadTimeEntriesFailure = createAction(
  '[Time Tracking] Load Time Entries Failure',
  props<{ error: string }>()
);

// Create Manual Time Entry
export const createManualTimeEntry = createAction(
  '[Time Tracking] Create Manual Entry',
  props<{ entryData: TimeEntryCreateRequest }>()
);

export const createManualTimeEntrySuccess = createAction(
  '[Time Tracking] Create Manual Entry Success',
  props<{ timeEntry: TimeEntry }>()
);

export const createManualTimeEntryFailure = createAction(
  '[Time Tracking] Create Manual Entry Failure',
  props<{ error: string }>()
);

// Delete Time Entry
export const deleteTimeEntry = createAction(
  '[Time Tracking] Delete Time Entry',
  props<{ id: string; reason?: string }>()
);

export const deleteTimeEntrySuccess = createAction(
  '[Time Tracking] Delete Time Entry Success',
  props<{ id: string }>()
);

export const deleteTimeEntryFailure = createAction(
  '[Time Tracking] Delete Time Entry Failure',
  props<{ error: string }>()
);

// Clear Errors
export const clearTimeTrackingError = createAction('[Time Tracking] Clear Error');

