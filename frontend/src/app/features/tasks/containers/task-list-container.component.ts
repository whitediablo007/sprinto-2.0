import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TaskCardComponent } from '../components/task-card.component';
import { TasksFacadeService } from '../services/tasks-facade.service';
import { TimerFacadeService } from '../../time-tracking/services/timer-facade.service';
import { Task } from '../../../shared/models/task.model';

/**
 * Task List Container Component (Smart).
 *
 * Container компонент для отображения списка задач.
 * Управляет состоянием через TasksFacadeService и TimerFacadeService.
 *
 * Отвечает за:
 * - Загрузку задач из store
 * - Делегирование действий (edit, delete, start timer) в services
 * - Навигацию к деталям задачи
 */
@Component({
  selector: 'app-task-list-container',
  standalone: true,
  imports: [CommonModule, TaskCardComponent],
  template: `
    <div class="container mx-auto px-4 py-6">
      <!-- Header -->
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-3xl font-bold text-gray-900">Мои задачи</h1>

        <button
          type="button"
          (click)="onCreateTask()"
          class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2">
          <span class="flex items-center gap-2">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" />
            </svg>
            Создать задачу
          </span>
        </button>
      </div>

      <!-- Statistics -->
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <div class="bg-white rounded-lg shadow p-4">
          <div class="text-sm text-gray-500">Всего задач</div>
          <div class="text-2xl font-bold text-gray-900">{{ (tasksCount$ | async) || 0 }}</div>
        </div>

        <div class="bg-white rounded-lg shadow p-4">
          <div class="text-sm text-gray-500">В работе</div>
          <div class="text-2xl font-bold text-blue-600">{{ (inProgressCount$ | async) || 0 }}</div>
        </div>

        <div class="bg-white rounded-lg shadow p-4">
          <div class="text-sm text-gray-500">Завершено</div>
          <div class="text-2xl font-bold text-green-600">{{ (completedCount$ | async) || 0 }}</div>
        </div>
      </div>

      <!-- Loading state -->
      <div *ngIf="loading$ | async" class="flex items-center justify-center py-12">
        <div class="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>

      <!-- Error state -->
      <div
        *ngIf="error$ | async as error"
        class="bg-red-50 border border-red-200 rounded-md p-4 mb-6"
        role="alert">
        <div class="flex items-start">
          <svg class="w-5 h-5 text-red-500 mr-3 mt-0.5" fill="currentColor" viewBox="0 0 20 20">
            <path
              fill-rule="evenodd"
              d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
              clip-rule="evenodd" />
          </svg>
          <div>
            <h3 class="text-sm font-medium text-red-800">Ошибка загрузки задач</h3>
            <p class="text-sm text-red-700 mt-1">{{ error }}</p>
            <button
              type="button"
              (click)="onRetry()"
              class="mt-2 text-sm text-red-600 hover:text-red-500 underline">
              Попробовать снова
            </button>
          </div>
        </div>
      </div>

      <!-- Empty state -->
      <div
        *ngIf="(tasks$ | async)?.length === 0 && (loading$ | async) === false"
        class="text-center py-12">
        <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
        <h3 class="mt-2 text-sm font-medium text-gray-900">Нет задач</h3>
        <p class="mt-1 text-sm text-gray-500">Начните с создания первой задачи</p>
        <button
          type="button"
          (click)="onCreateTask()"
          class="mt-4 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors">
          Создать задачу
        </button>
      </div>

      <!-- Task list -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <app-task-card
          *ngFor="let task of tasks$ | async; trackBy: trackByTaskId"
          [task]="task"
          [isTimerRunning]="(hasActiveTimer$ | async) || false"
          (edit)="onEditTask($event)"
          (delete)="onDeleteTask($event)"
          (startTimer)="onStartTimer($event)"
          (cardClick)="onTaskClick($event)">
        </app-task-card>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }
    `,
  ],
})
export class TaskListContainerComponent implements OnInit {
  private tasksFacade = inject(TasksFacadeService);
  private timerFacade = inject(TimerFacadeService);
  private router = inject(Router);

  // Observables from facade
  tasks$ = this.tasksFacade.tasks$;
  loading$ = this.tasksFacade.loading$;
  error$ = this.tasksFacade.error$;
  tasksCount$ = this.tasksFacade.tasksCount$;
  completedCount$ = this.tasksFacade.completedTasksCount$;
  inProgressCount$ = this.tasksFacade.inProgressTasksCount$;
  hasActiveTimer$ = this.timerFacade.hasActiveTimer$;

  ngOnInit(): void {
    // Load tasks on init (assigned to current user)
    this.tasksFacade.loadTasks();

    // Note: Active timer is loaded globally by TimerContainerComponent
  }

  onCreateTask(): void {
    this.router.navigate(['/tasks/new']);
  }

  onEditTask(task: Task): void {
    this.router.navigate(['/tasks', task.id, 'edit']);
  }

  onDeleteTask(task: Task): void {
    if (confirm(`Вы уверены, что хотите удалить задачу "${task.title}"?`)) {
      this.tasksFacade.deleteTask(task.id);
    }
  }

  onStartTimer(taskId: string): void {
    this.timerFacade.startTimer(taskId);
  }

  onTaskClick(task: Task): void {
    this.router.navigate(['/tasks', task.id]);
  }

  onRetry(): void {
    this.tasksFacade.clearError();
    this.tasksFacade.loadTasks();
  }

  trackByTaskId(_index: number, task: Task): string {
    return task.id;
  }
}

