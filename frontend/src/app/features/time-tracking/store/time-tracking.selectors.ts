import { createFeatureSelector, createSelector } from '@ngrx/store';
import { TimeTrackingState } from './time-tracking.reducer';

/**
 * Time Tracking Selectors для NgRx Store.
 *
 * Предоставляет мемоизированные селекторы для извлечения данных из time-tracking state.
 */

export const selectTimeTrackingFeature = createFeatureSelector<TimeTrackingState>('timeTracking');

// Base selectors
export const selectActiveTimer = createSelector(selectTimeTrackingFeature, (state) => state.activeTimer);

export const selectTimerUpdate = createSelector(selectTimeTrackingFeature, (state) => state.timerUpdate);

export const selectAllTimeEntries = createSelector(selectTimeTrackingFeature, (state) => state.timeEntries);

export const selectTimeTrackingLoading = createSelector(selectTimeTrackingFeature, (state) => state.loading);

export const selectTimeTrackingError = createSelector(selectTimeTrackingFeature, (state) => state.error);

export const selectLastUpdated = createSelector(selectTimeTrackingFeature, (state) => state.lastUpdated);

// Derived selectors
export const selectHasActiveTimer = createSelector(selectActiveTimer, (timer) => timer !== null);

export const selectTimerElapsedSeconds = createSelector(
  selectTimerUpdate,
  (update) => update?.elapsedSeconds || 0
);

export const selectTimerTaskTitle = createSelector(selectTimerUpdate, (update) => update?.taskTitle || '');

export const selectTimeEntriesByTaskId = (taskId: string) =>
  createSelector(selectAllTimeEntries, (entries) => entries.filter((entry) => entry.taskId === taskId));

export const selectTimeEntriesByUserId = (userId: string) =>
  createSelector(selectAllTimeEntries, (entries) => entries.filter((entry) => entry.userId === userId));

export const selectActiveTimeEntries = createSelector(selectAllTimeEntries, (entries) =>
  entries.filter((entry) => entry.endTime === null)
);

export const selectCompletedTimeEntries = createSelector(selectAllTimeEntries, (entries) =>
  entries.filter((entry) => entry.endTime !== null)
);

// Statistics selectors
export const selectTotalTrackedSeconds = createSelector(selectAllTimeEntries, (entries) =>
  entries.reduce((total, entry) => total + (entry.durationSeconds || 0), 0)
);

export const selectTotalCost = createSelector(selectAllTimeEntries, (entries) =>
  entries.reduce((total, entry) => total + (entry.cost || 0), 0)
);

export const selectTaskTotalTime = (taskId: string) =>
  createSelector(selectTimeEntriesByTaskId(taskId), (entries) =>
    entries.reduce((total, entry) => total + (entry.durationSeconds || 0), 0)
  );

// UI state selectors
export const selectTimerViewModel = createSelector(
  selectActiveTimer,
  selectTimerUpdate,
  selectTimeTrackingLoading,
  selectTimeTrackingError,
  (activeTimer, timerUpdate, loading, error) => ({
    activeTimer,
    timerUpdate,
    loading,
    error,
    isRunning: activeTimer !== null,
    elapsedSeconds: timerUpdate?.elapsedSeconds || 0,
    taskTitle: timerUpdate?.taskTitle || activeTimer?.taskTitle || '',
    projectName: timerUpdate?.projectName || '',
    projectColor: timerUpdate?.projectColor || '',
  })
);

export const selectTimeEntriesViewModel = createSelector(
  selectAllTimeEntries,
  selectTimeTrackingLoading,
  selectTimeTrackingError,
  (timeEntries, loading, error) => ({
    timeEntries,
    loading,
    error,
    isEmpty: timeEntries.length === 0 && !loading,
    hasError: !!error,
  })
);

