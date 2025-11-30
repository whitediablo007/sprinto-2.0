import { Routes } from '@angular/router';
import { TaskListContainerComponent } from './containers/task-list-container.component';
import { TaskDetailsContainerComponent } from './containers/task-details-container.component';

/**
 * Tasks Feature Routes.
 *
 * Определяет маршруты для feature tasks:
 * - /tasks - список задач
 * - /tasks/new - создание новой задачи
 * - /tasks/:id - детали задачи
 * - /tasks/:id/edit - редактирование задачи
 *
 * Использует lazy loading для оптимизации bundle size.
 */
export const TASKS_ROUTES: Routes = [
  {
    path: '',
    component: TaskListContainerComponent,
  },
  {
    path: 'new',
    component: TaskDetailsContainerComponent,
    data: { title: 'Create Task' },
  },
  {
    path: ':id',
    component: TaskDetailsContainerComponent,
    data: { title: 'Task Details' },
  },
  {
    path: ':id/edit',
    component: TaskDetailsContainerComponent,
    data: { title: 'Edit Task' },
  },
];

