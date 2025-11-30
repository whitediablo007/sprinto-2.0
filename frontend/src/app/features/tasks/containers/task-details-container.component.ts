import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { TaskFormComponent } from '../components/task-form.component';
import { TaskStatusBadgeComponent } from '../../../shared/components/task-status-badge.component';
import { TasksFacadeService } from '../services/tasks-facade.service';
import { TimerFacadeService } from '../../time-tracking/services/timer-facade.service';
import { Task, TaskCreateRequest } from '../../../shared/models/task.model';
import { filter, switchMap } from 'rxjs/operators';

/**
 * Task Details Container Component (Smart).
 *
 * Container компонент для отображения деталей задачи.
 * Поддерживает режимы:
 * - Просмотр деталей задачи
 * - Редактирование существующей задачи
 * - Создание новой задачи
 *
 * Управляет состоянием через TasksFacadeService.
 */
@Component({
  selector: 'app-task-details-container',
  standalone: true,
  imports: [CommonModule, TaskFormComponent, TaskStatusBadgeComponent],
  template: `
    <div class="container mx-auto px-4 py-6 max-w-4xl">
      <!-- Header -->
      <div class="flex items-center gap-4 mb-6">
        <button
          type="button"
          (click)="onBack()"
          class="p-2 text-gray-500 hover:text-gray-700 rounded-md hover:bg-gray-100 transition-colors"
          aria-label="Go back">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
        </button>

        <h1 class="text-3xl font-bold text-gray-900">
          {{ isEditMode() ? 'Редактирование задачи' : isCreateMode() ? 'Новая задача' : 'Детали задачи' }}
        </h1>
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
        <p class="text-sm text-red-700">{{ error }}</p>
      </div>

      <!-- Create/Edit Mode -->
      <div *ngIf="isEditMode() || isCreateMode()" class="bg-white rounded-lg shadow-md p-6">
        <app-task-form
          [editMode]="isEditMode()"
          [task]="task$ | async"
          [projectId]="projectId()"
          [isSubmitting]="(loading$ | async) || false"
          (formSubmit)="onFormSubmit($event)"
          (formCancel)="onFormCancel()">
        </app-task-form>
      </div>

      <!-- View Mode -->
      <div *ngIf="isViewMode() && (task$ | async) as task" class="bg-white rounded-lg shadow-md p-6 space-y-6">
        <!-- Task Header -->
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <h2 class="text-2xl font-bold text-gray-900 mb-2">{{ task.title }}</h2>
            <div class="flex items-center gap-3">
              <app-task-status-badge [status]="task.status"></app-task-status-badge>
              <span class="text-sm text-gray-500">Создана {{ formatDate(task.createdAt) }}</span>
            </div>
          </div>

          <div class="flex gap-2">
            <button
              type="button"
              (click)="onEdit()"
              class="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors">
              Редактировать
            </button>

            <button
              *ngIf="(hasActiveTimer$ | async) === false"
              type="button"
              (click)="onStartTimer(task.id)"
              class="px-4 py-2 bg-green-600 text-white rounded-md hover:bg-green-700 transition-colors">
              Запустить таймер
            </button>
          </div>
        </div>

        <!-- Task Details -->
        <div class="space-y-4">
          <!-- Description -->
          <div *ngIf="task.description">
            <h3 class="text-sm font-medium text-gray-700 mb-2">Описание</h3>
            <p class="text-gray-900 whitespace-pre-wrap">{{ task.description }}</p>
          </div>

          <!-- Metadata Grid -->
          <div class="grid grid-cols-2 gap-4 pt-4 border-t border-gray-200">
            <div>
              <h4 class="text-sm font-medium text-gray-700 mb-1">Приоритет</h4>
              <p class="text-gray-900">{{ getPriorityLabel(task.priority) }}</p>
            </div>

            <div *ngIf="task.assigneeName">
              <h4 class="text-sm font-medium text-gray-700 mb-1">Исполнитель</h4>
              <p class="text-gray-900">{{ task.assigneeName }}</p>
            </div>

            <div *ngIf="task.estimatedHours">
              <h4 class="text-sm font-medium text-gray-700 mb-1">Оценка</h4>
              <p class="text-gray-900">{{ task.estimatedHours }} часов</p>
            </div>

            <div *ngIf="task.deadline">
              <h4 class="text-sm font-medium text-gray-700 mb-1">Дедлайн</h4>
              <p class="text-gray-900">{{ formatDate(task.deadline) }}</p>
            </div>

            <div *ngIf="task.completedAt">
              <h4 class="text-sm font-medium text-gray-700 mb-1">Завершена</h4>
              <p class="text-gray-900">{{ formatDate(task.completedAt) }}</p>
            </div>

            <div>
              <h4 class="text-sm font-medium text-gray-700 mb-1">Создатель</h4>
              <p class="text-gray-900">{{ task.createdByName || 'Неизвестно' }}</p>
            </div>
          </div>

          <!-- Subtasks Section -->
          <div *ngIf="(subtasks$ | async)?.length" class="pt-4 border-t border-gray-200">
            <h3 class="text-lg font-semibold text-gray-900 mb-3">Подзадачи</h3>
            <div class="space-y-2">
              <div
                *ngFor="let subtask of subtasks$ | async"
                class="flex items-center justify-between p-3 bg-gray-50 rounded-md hover:bg-gray-100 cursor-pointer"
                (click)="onSubtaskClick(subtask.id)"
                (keydown.enter)="onSubtaskClick(subtask.id)"
                tabindex="0"
                role="button">
                <span class="text-gray-900">{{ subtask.title }}</span>
                <app-task-status-badge [status]="subtask.status"></app-task-status-badge>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class TaskDetailsContainerComponent implements OnInit {
  private tasksFacade = inject(TasksFacadeService);
  private timerFacade = inject(TimerFacadeService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  // UI state signals
  private mode = signal<'view' | 'edit' | 'create'>('view');
  private taskId = signal<string | null>(null);
  projectId = signal<string | null>(null);

  // Observables from facade
  task$ = this.tasksFacade.selectedTask$;
  loading$ = this.tasksFacade.loading$;
  error$ = this.tasksFacade.error$;
  hasActiveTimer$ = this.timerFacade.hasActiveTimer$;
  subtasks$ = this.route.params.pipe(
    switchMap((params) => {
      const id = params['id'];
      return id ? this.tasksFacade.getSubtasks(id) : [];
    })
  );

  ngOnInit(): void {
    // Check if we're in create mode by looking at the URL path
    const urlSegments = this.route.snapshot.url;
    const isNewRoute = urlSegments.some((segment) => segment.path === 'new');

    if (isNewRoute) {
      // Create mode
      this.mode.set('create');
      this.taskId.set(null);
      // Get projectId from query params
      this.route.queryParams.subscribe((queryParams) => {
        this.projectId.set(queryParams['projectId'] || null);
      });
    } else {
      // Edit or View mode
      this.route.params.subscribe((params) => {
        const id = params['id'];
        if (id) {
          this.taskId.set(id);
          this.tasksFacade.selectTask(id);
          this.tasksFacade.loadTask(id);
          this.tasksFacade.loadSubtasks(id);

          // Check route for edit mode
          if (urlSegments.some((segment) => segment.path === 'edit')) {
            this.mode.set('edit');
          } else {
            this.mode.set('view');
          }
        }
      });
    }

    // Note: Active timer is loaded globally by TimerContainerComponent
  }

  isViewMode(): boolean {
    return this.mode() === 'view';
  }

  isEditMode(): boolean {
    return this.mode() === 'edit';
  }

  isCreateMode(): boolean {
    return this.mode() === 'create';
  }

  onFormSubmit(taskData: TaskCreateRequest): void {
    if (this.isEditMode() && this.taskId()) {
      this.tasksFacade.updateTask(this.taskId()!, taskData);
      // Wait for success and navigate back
      setTimeout(() => this.onBack(), 500);
    } else if (this.isCreateMode()) {
      this.tasksFacade.createTask(taskData);
      // Wait for success and navigate back
      setTimeout(() => this.router.navigate(['/tasks']), 500);
    }
  }

  onFormCancel(): void {
    this.onBack();
  }

  onEdit(): void {
    if (this.taskId()) {
      this.router.navigate(['/tasks', this.taskId(), 'edit']);
    }
  }

  onStartTimer(taskId: string): void {
    this.timerFacade.startTimer(taskId);
  }

  onBack(): void {
    this.router.navigate(['/tasks']);
  }

  onSubtaskClick(subtaskId: string): void {
    this.router.navigate(['/tasks', subtaskId]);
  }

  getPriorityLabel(priority: string): string {
    const labels: Record<string, string> = {
      URGENT: 'Срочно',
      HIGH: 'Высокий',
      MEDIUM: 'Средний',
      LOW: 'Низкий',
    };
    return labels[priority] || priority;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}

