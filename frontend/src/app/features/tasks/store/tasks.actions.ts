import { createAction, props } from '@ngrx/store';
import { Task, TaskCreateRequest, TaskUpdateStatusRequest } from '../../../shared/models/task.model';

/**
 * Tasks Actions для NgRx Store.
 *
 * Определяет все действия для управления задачами:
 * - CRUD операции (create, load, update, delete)
 * - Управление статусами
 * - Фильтрация и поиск
 * - Подзадачи
 */

// Load Tasks Actions
export const loadTasks = createAction(
  '[Tasks] Load Tasks',
  props<{ projectId?: string; assigneeId?: string }>()
);

export const loadTasksSuccess = createAction(
  '[Tasks] Load Tasks Success',
  props<{ tasks: Task[] }>()
);

export const loadTasksFailure = createAction(
  '[Tasks] Load Tasks Failure',
  props<{ error: string }>()
);

// Load Single Task
export const loadTask = createAction(
  '[Tasks] Load Task',
  props<{ id: string }>()
);

export const loadTaskSuccess = createAction(
  '[Tasks] Load Task Success',
  props<{ task: Task }>()
);

export const loadTaskFailure = createAction(
  '[Tasks] Load Task Failure',
  props<{ error: string }>()
);

// Create Task
export const createTask = createAction(
  '[Tasks] Create Task',
  props<{ taskData: TaskCreateRequest }>()
);

export const createTaskSuccess = createAction(
  '[Tasks] Create Task Success',
  props<{ task: Task }>()
);

export const createTaskFailure = createAction(
  '[Tasks] Create Task Failure',
  props<{ error: string }>()
);

// Update Task
export const updateTask = createAction(
  '[Tasks] Update Task',
  props<{ id: string; taskData: Partial<TaskCreateRequest> }>()
);

export const updateTaskSuccess = createAction(
  '[Tasks] Update Task Success',
  props<{ task: Task }>()
);

export const updateTaskFailure = createAction(
  '[Tasks] Update Task Failure',
  props<{ error: string }>()
);

// Update Task Status
export const updateTaskStatus = createAction(
  '[Tasks] Update Task Status',
  props<{ id: string; statusUpdate: TaskUpdateStatusRequest }>()
);

export const updateTaskStatusSuccess = createAction(
  '[Tasks] Update Task Status Success',
  props<{ task: Task }>()
);

export const updateTaskStatusFailure = createAction(
  '[Tasks] Update Task Status Failure',
  props<{ error: string }>()
);

// Delete Task
export const deleteTask = createAction(
  '[Tasks] Delete Task',
  props<{ id: string }>()
);

export const deleteTaskSuccess = createAction(
  '[Tasks] Delete Task Success',
  props<{ id: string }>()
);

export const deleteTaskFailure = createAction(
  '[Tasks] Delete Task Failure',
  props<{ error: string }>()
);

// Load Subtasks
export const loadSubtasks = createAction(
  '[Tasks] Load Subtasks',
  props<{ parentTaskId: string }>()
);

export const loadSubtasksSuccess = createAction(
  '[Tasks] Load Subtasks Success',
  props<{ subtasks: Task[] }>()
);

export const loadSubtasksFailure = createAction(
  '[Tasks] Load Subtasks Failure',
  props<{ error: string }>()
);

// Select Task (for details view)
export const selectTask = createAction(
  '[Tasks] Select Task',
  props<{ id: string | null }>()
);

// Clear Errors
export const clearTasksError = createAction('[Tasks] Clear Error');

