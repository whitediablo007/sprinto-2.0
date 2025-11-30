import { createReducer, on } from '@ngrx/store';
import { TimeEntry, TimerUpdate } from '../../../shared/models/time-entry.model';
import * as TimeTrackingActions from './time-tracking.actions';

/**
 * Time Tracking State interface.
 */
export interface TimeTrackingState {
  activeTimer: TimeEntry | null;
  timerUpdate: TimerUpdate | null;
  timeEntries: TimeEntry[];
  loading: boolean;
  error: string | null;
  lastUpdated: number | null;
}

/**
 * Initial time tracking state.
 */
export const initialTimeTrackingState: TimeTrackingState = {
  activeTimer: null,
  timerUpdate: null,
  timeEntries: [],
  loading: false,
  error: null,
  lastUpdated: null,
};

/**
 * Time Tracking Reducer.
 *
 * Обрабатывает изменения состояния для операций с таймерами и записями времени.
 */
export const timeTrackingReducer = createReducer(
  initialTimeTrackingState,

  // Start Timer
  on(TimeTrackingActions.startTimer, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TimeTrackingActions.startTimerSuccess, (state, { timeEntry }) => ({
    ...state,
    activeTimer: timeEntry,
    timeEntries: [...state.timeEntries, timeEntry],
    loading: false,
    error: null,
  })),

  on(TimeTrackingActions.startTimerFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Stop Timer
  on(TimeTrackingActions.stopTimer, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TimeTrackingActions.stopTimerSuccess, (state, { timeEntry }) => ({
    ...state,
    activeTimer: null,
    timeEntries: state.timeEntries.map((entry) => (entry.id === timeEntry.id ? timeEntry : entry)),
    timerUpdate: null,
    loading: false,
    error: null,
  })),

  on(TimeTrackingActions.stopTimerFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Load Active Timer
  on(TimeTrackingActions.loadActiveTimer, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TimeTrackingActions.loadActiveTimerSuccess, (state, { timeEntry }) => ({
    ...state,
    activeTimer: timeEntry,
    loading: false,
    error: null,
  })),

  on(TimeTrackingActions.loadActiveTimerFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // WebSocket Timer Update
  on(TimeTrackingActions.timerUpdate, (state, { update }) => ({
    ...state,
    timerUpdate: update,
  })),

  // Load Time Entries
  on(TimeTrackingActions.loadTimeEntries, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TimeTrackingActions.loadTimeEntriesSuccess, (state, { timeEntries }) => ({
    ...state,
    timeEntries,
    loading: false,
    error: null,
    lastUpdated: Date.now(),
  })),

  on(TimeTrackingActions.loadTimeEntriesFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Create Manual Time Entry
  on(TimeTrackingActions.createManualTimeEntry, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TimeTrackingActions.createManualTimeEntrySuccess, (state, { timeEntry }) => ({
    ...state,
    timeEntries: [...state.timeEntries, timeEntry],
    loading: false,
    error: null,
  })),

  on(TimeTrackingActions.createManualTimeEntryFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Delete Time Entry
  on(TimeTrackingActions.deleteTimeEntry, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TimeTrackingActions.deleteTimeEntrySuccess, (state, { id }) => ({
    ...state,
    timeEntries: state.timeEntries.filter((entry) => entry.id !== id),
    loading: false,
    error: null,
  })),

  on(TimeTrackingActions.deleteTimeEntryFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Clear Error
  on(TimeTrackingActions.clearTimeTrackingError, (state) => ({
    ...state,
    error: null,
  }))
);

