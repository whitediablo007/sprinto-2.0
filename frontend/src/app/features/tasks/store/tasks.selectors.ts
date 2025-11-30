import { createFeatureSelector, createSelector } from '@ngrx/store';
import { TasksState } from './tasks.reducer';
import { Task, TaskStatus } from '../../../shared/models/task.model';

/**
 * Tasks Selectors для NgRx Store.
 *
 * Предоставляет мемоизированные селекторы для извлечения данных из tasks state.
 */

export const selectTasksFeature = createFeatureSelector<TasksState>('tasks');

// Base selectors
export const selectAllTasks = createSelector(
  selectTasksFeature,
  (state) => state.tasks
);

export const selectSelectedTaskId = createSelector(
  selectTasksFeature,
  (state) => state.selectedTaskId
);

export const selectTasksLoading = createSelector(
  selectTasksFeature,
  (state) => state.loading
);

export const selectTasksError = createSelector(
  selectTasksFeature,
  (state) => state.error
);

export const selectLastUpdated = createSelector(
  selectTasksFeature,
  (state) => state.lastUpdated
);

// Derived selectors
export const selectSelectedTask = createSelector(
  selectAllTasks,
  selectSelectedTaskId,
  (tasks, selectedId) => tasks.find((task) => task.id === selectedId) || null
);

export const selectTaskById = (id: string) =>
  createSelector(selectAllTasks, (tasks) => tasks.find((task) => task.id === id) || null);

export const selectTasksByProjectId = (projectId: string) =>
  createSelector(selectAllTasks, (tasks) => tasks.filter((task) => task.projectId === projectId));

export const selectTasksByAssigneeId = (assigneeId: string) =>
  createSelector(selectAllTasks, (tasks) => tasks.filter((task) => task.assigneeId === assigneeId));

export const selectTasksByStatus = (status: TaskStatus) =>
  createSelector(selectAllTasks, (tasks) => tasks.filter((task) => task.status === status));

export const selectSubtasks = (parentTaskId: string) =>
  createSelector(selectAllTasks, (tasks) => tasks.filter((task) => task.parentTaskId === parentTaskId));

export const selectRootTasksByProjectId = (projectId: string) =>
  createSelector(selectAllTasks, (tasks) =>
    tasks.filter((task) => task.projectId === projectId && !task.parentTaskId)
  );

// Statistics selectors
export const selectTasksCount = createSelector(selectAllTasks, (tasks) => tasks.length);

export const selectCompletedTasksCount = createSelector(
  selectAllTasks,
  (tasks) => tasks.filter((task) => task.status === TaskStatus.COMPLETED).length
);

export const selectInProgressTasksCount = createSelector(
  selectAllTasks,
  (tasks) => tasks.filter((task) => task.status === TaskStatus.IN_PROGRESS).length
);

export const selectTasksCompletionRate = createSelector(
  selectTasksCount,
  selectCompletedTasksCount,
  (total, completed) => (total > 0 ? (completed / total) * 100 : 0)
);

// UI state selectors
export const selectTasksViewModel = createSelector(
  selectAllTasks,
  selectTasksLoading,
  selectTasksError,
  selectSelectedTaskId,
  (tasks, loading, error, selectedTaskId) => ({
    tasks,
    loading,
    error,
    selectedTaskId,
    isEmpty: tasks.length === 0 && !loading,
    hasError: !!error,
  })
);

