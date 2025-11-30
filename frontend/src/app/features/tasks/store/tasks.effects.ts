import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import * as TasksActions from './tasks.actions';
import { TasksApiService } from '../services/tasks-api.service';

/**
 * Tasks Effects.
 *
 * Обрабатывает side effects для операций с задачами:
 * - API вызовы для CRUD операций
 * - Обработка ошибок
 * - Загрузка подзадач
 *
 * Использует TasksApiService для HTTP запросов.
 */
@Injectable()
export class TasksEffects {
  private actions$ = inject(Actions);
  private tasksApiService = inject(TasksApiService);

  /**
   * Load tasks effect.
   * Загружает список задач по фильтрам (projectId, assigneeId).
   */
  loadTasks$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TasksActions.loadTasks),
      switchMap(({ projectId, assigneeId }) => {
        return this.tasksApiService.getTasks({ projectId, assigneeId }).pipe(
          map((tasks) => TasksActions.loadTasksSuccess({ tasks })),
          catchError((error) =>
            of(TasksActions.loadTasksFailure({ error: error.message || 'Failed to load tasks' }))
          )
        );
      })
    )
  );

  /**
   * Load single task effect.
   */
  loadTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TasksActions.loadTask),
      switchMap(({ id }) => {
        return this.tasksApiService.getTaskById(id).pipe(
          map((task) => TasksActions.loadTaskSuccess({ task })),
          catchError((error) =>
            of(TasksActions.loadTaskFailure({ error: error.message || 'Failed to load task' }))
          )
        );
      })
    )
  );

  /**
   * Create task effect.
   */
  createTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TasksActions.createTask),
      switchMap(({ taskData }) => {
        return this.tasksApiService.createTask(taskData).pipe(
          map((task) => TasksActions.createTaskSuccess({ task })),
          catchError((error) =>
            of(TasksActions.createTaskFailure({ error: error.message || 'Failed to create task' }))
          )
        );
      })
    )
  );

  /**
   * Update task effect.
   */
  updateTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TasksActions.updateTask),
      switchMap(({ id, taskData }) => {
        return this.tasksApiService.updateTask(id, taskData).pipe(
          map((task) => TasksActions.updateTaskSuccess({ task })),
          catchError((error) =>
            of(TasksActions.updateTaskFailure({ error: error.message || 'Failed to update task' }))
          )
        );
      })
    )
  );

  /**
   * Update task status effect.
   */
  updateTaskStatus$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TasksActions.updateTaskStatus),
      switchMap(({ id, statusUpdate }) => {
        return this.tasksApiService.updateTaskStatus(id, statusUpdate).pipe(
          map((task) => TasksActions.updateTaskStatusSuccess({ task })),
          catchError((error) =>
            of(
              TasksActions.updateTaskStatusFailure({
                error: error.message || 'Failed to update task status',
              })
            )
          )
        );
      })
    )
  );

  /**
   * Delete task effect.
   */
  deleteTask$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TasksActions.deleteTask),
      switchMap(({ id }) => {
        return this.tasksApiService.deleteTask(id).pipe(
          map(() => TasksActions.deleteTaskSuccess({ id })),
          catchError((error) =>
            of(TasksActions.deleteTaskFailure({ error: error.message || 'Failed to delete task' }))
          )
        );
      })
    )
  );

  /**
   * Load subtasks effect.
   */
  loadSubtasks$ = createEffect(() =>
    this.actions$.pipe(
      ofType(TasksActions.loadSubtasks),
      switchMap(({ parentTaskId }) => {
        return this.tasksApiService.getSubtasks(parentTaskId).pipe(
          map((subtasks) => TasksActions.loadSubtasksSuccess({ subtasks })),
          catchError((error) =>
            of(TasksActions.loadSubtasksFailure({ error: error.message || 'Failed to load subtasks' }))
          )
        );
      })
    )
  );
}

