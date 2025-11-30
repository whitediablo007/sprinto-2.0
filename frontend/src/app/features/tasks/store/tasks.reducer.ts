import { createReducer, on } from '@ngrx/store';
import { Task } from '../../../shared/models/task.model';
import * as TasksActions from './tasks.actions';

/**
 * Tasks State interface.
 */
export interface TasksState {
  tasks: Task[];
  selectedTaskId: string | null;
  loading: boolean;
  error: string | null;
  lastUpdated: number | null;
}

/**
 * Initial tasks state.
 */
export const initialTasksState: TasksState = {
  tasks: [],
  selectedTaskId: null,
  loading: false,
  error: null,
  lastUpdated: null,
};

/**
 * Tasks Reducer.
 *
 * Обрабатывает изменения состояния для операций с задачами.
 */
export const tasksReducer = createReducer(
  initialTasksState,

  // Load Tasks
  on(TasksActions.loadTasks, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TasksActions.loadTasksSuccess, (state, { tasks }) => ({
    ...state,
    tasks,
    loading: false,
    error: null,
    lastUpdated: Date.now(),
  })),

  on(TasksActions.loadTasksFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Load Single Task
  on(TasksActions.loadTask, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TasksActions.loadTaskSuccess, (state, { task }) => {
    const existingIndex = state.tasks.findIndex((t) => t.id === task.id);
    const updatedTasks =
      existingIndex >= 0
        ? state.tasks.map((t, idx) => (idx === existingIndex ? task : t))
        : [...state.tasks, task];

    return {
      ...state,
      tasks: updatedTasks,
      loading: false,
      error: null,
    };
  }),

  on(TasksActions.loadTaskFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Create Task
  on(TasksActions.createTask, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TasksActions.createTaskSuccess, (state, { task }) => ({
    ...state,
    tasks: [...state.tasks, task],
    loading: false,
    error: null,
  })),

  on(TasksActions.createTaskFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Update Task
  on(TasksActions.updateTask, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TasksActions.updateTaskSuccess, (state, { task }) => ({
    ...state,
    tasks: state.tasks.map((t) => (t.id === task.id ? task : t)),
    loading: false,
    error: null,
  })),

  on(TasksActions.updateTaskFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Update Task Status
  on(TasksActions.updateTaskStatus, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TasksActions.updateTaskStatusSuccess, (state, { task }) => ({
    ...state,
    tasks: state.tasks.map((t) => (t.id === task.id ? task : t)),
    loading: false,
    error: null,
  })),

  on(TasksActions.updateTaskStatusFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Delete Task
  on(TasksActions.deleteTask, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TasksActions.deleteTaskSuccess, (state, { id }) => ({
    ...state,
    tasks: state.tasks.filter((t) => t.id !== id),
    selectedTaskId: state.selectedTaskId === id ? null : state.selectedTaskId,
    loading: false,
    error: null,
  })),

  on(TasksActions.deleteTaskFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Load Subtasks
  on(TasksActions.loadSubtasks, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),

  on(TasksActions.loadSubtasksSuccess, (state, { subtasks }) => {
    // Merge subtasks into existing tasks array
    const subtaskIds = new Set(subtasks.map((s) => s.id));
    const tasksWithoutSubtasks = state.tasks.filter((t) => !subtaskIds.has(t.id));

    return {
      ...state,
      tasks: [...tasksWithoutSubtasks, ...subtasks],
      loading: false,
      error: null,
    };
  }),

  on(TasksActions.loadSubtasksFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),

  // Select Task
  on(TasksActions.selectTask, (state, { id }) => ({
    ...state,
    selectedTaskId: id,
  })),

  // Clear Error
  on(TasksActions.clearTasksError, (state) => ({
    ...state,
    error: null,
  }))
);

