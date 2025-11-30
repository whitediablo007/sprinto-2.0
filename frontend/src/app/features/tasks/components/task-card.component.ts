import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Task, TaskStatus, TaskPriority } from '../../../shared/models/task.model';
import { TaskStatusBadgeComponent } from '../../../shared/components/task-status-badge.component';

/**
 * Task Card Component (Presentational).
 *
 * Отображает карточку задачи с основной информацией.
 * Presentational component - вся логика и данные передаются через @Input/@Output.
 *
 * Использует OnPush change detection для максимальной производительности.
 */
@Component({
  selector: 'app-task-card',
  standalone: true,
  imports: [CommonModule, TaskStatusBadgeComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="bg-white rounded-lg shadow-md p-4 hover:shadow-lg transition-shadow border-l-4 cursor-pointer"
      [ngClass]="getPriorityBorderClass()"
      (click)="onCardClick()"
      (keydown.enter)="onCardClick()"
      tabindex="0"
      role="button"
      [attr.aria-label]="'Task: ' + task.title">
      <!-- Header with title and actions -->
      <div class="flex items-start justify-between mb-3">
        <div class="flex-1 mr-3">
          <h3 class="text-lg font-semibold text-gray-900 mb-1" [title]="task.title">
            {{ task.title }}
          </h3>

          <!-- Task hierarchy indicator -->
          <div *ngIf="task.hierarchyLevel > 0" class="flex items-center text-xs text-gray-500 mb-2">
            <svg class="w-3 h-3 mr-1" fill="currentColor" viewBox="0 0 20 20">
              <path
                d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" />
            </svg>
            Подзадача (уровень {{ task.hierarchyLevel }})
          </div>
        </div>

        <!-- Actions menu -->
        <div class="flex gap-2" (click)="$event.stopPropagation()" (keydown.enter)="$event.stopPropagation()" role="group">
          <button
            type="button"
            (click)="onEdit()"
            class="p-2 text-gray-500 hover:text-blue-600 rounded-md hover:bg-gray-100 transition-colors"
            [attr.aria-label]="'Edit task ' + task.title">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
          </button>

          <button
            type="button"
            (click)="onDelete()"
            class="p-2 text-gray-500 hover:text-red-600 rounded-md hover:bg-gray-100 transition-colors"
            [attr.aria-label]="'Delete task ' + task.title">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
          </button>

          <button
            *ngIf="!isTimerRunning"
            type="button"
            (click)="onStartTimer()"
            class="p-2 text-gray-500 hover:text-green-600 rounded-md hover:bg-gray-100 transition-colors"
            [attr.aria-label]="'Start timer for ' + task.title">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </button>
        </div>
      </div>

      <!-- Description (truncated) -->
      <p *ngIf="task.description" class="text-sm text-gray-600 mb-3 line-clamp-2" [title]="task.description">
        {{ task.description }}
      </p>

      <!-- Status and Priority badges -->
      <div class="flex items-center gap-2 mb-3">
        <app-task-status-badge [status]="task.status"></app-task-status-badge>

        <span
          class="px-2 py-1 rounded text-xs font-medium"
          [ngClass]="getPriorityBadgeClass()"
          [attr.aria-label]="'Priority: ' + getPriorityLabel()">
          {{ getPriorityLabel() }}
        </span>
      </div>

      <!-- Metadata (assignee, deadline, etc.) -->
      <div class="flex items-center justify-between text-sm text-gray-500">
        <div class="flex items-center gap-4">
          <!-- Assignee -->
          <div *ngIf="task.assigneeName" class="flex items-center gap-1" [title]="'Assigned to: ' + task.assigneeName">
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path
                fill-rule="evenodd"
                d="M10 9a3 3 0 100-6 3 3 0 000 6zm-7 9a7 7 0 1114 0H3z"
                clip-rule="evenodd" />
            </svg>
            <span>{{ task.assigneeName }}</span>
          </div>

          <!-- Estimated hours -->
          <div *ngIf="task.estimatedHours" class="flex items-center gap-1" [title]="'Estimated: ' + task.estimatedHours + ' hours'">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>{{ task.estimatedHours }}ч</span>
          </div>
        </div>

        <!-- Deadline -->
        <div *ngIf="task.deadline" class="flex items-center gap-1" [class.text-red-500]="isOverdue()" [title]="'Deadline: ' + formatDate(task.deadline)">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
          <span>{{ formatDate(task.deadline) }}</span>
        </div>
      </div>

      <!-- Subtasks indicator (if any) -->
      <div *ngIf="task.subtasksCount && task.subtasksCount > 0" class="mt-3 pt-3 border-t border-gray-200">
        <div class="flex items-center justify-between text-sm">
          <span class="text-gray-600">Подзадачи:</span>
          <span class="font-medium">
            {{ task.completedSubtasksCount || 0 }} / {{ task.subtasksCount }}
          </span>
        </div>
        <!-- Progress bar -->
        <div class="w-full bg-gray-200 rounded-full h-2 mt-2">
          <div class="bg-green-500 h-2 rounded-full transition-all" [style.width.%]="task.progressPercent || 0"></div>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .line-clamp-2 {
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    `,
  ],
})
export class TaskCardComponent {
  @Input({ required: true }) task!: Task;
  @Input() isTimerRunning: boolean = false;

  @Output() edit = new EventEmitter<Task>();
  @Output() delete = new EventEmitter<Task>();
  @Output() startTimer = new EventEmitter<string>();
  @Output() cardClick = new EventEmitter<Task>();

  onEdit(): void {
    this.edit.emit(this.task);
  }

  onDelete(): void {
    this.delete.emit(this.task);
  }

  onStartTimer(): void {
    this.startTimer.emit(this.task.id);
  }

  onCardClick(): void {
    this.cardClick.emit(this.task);
  }

  getPriorityBorderClass(): string {
    switch (this.task.priority) {
      case TaskPriority.URGENT:
        return 'border-red-600';
      case TaskPriority.HIGH:
        return 'border-orange-500';
      case TaskPriority.MEDIUM:
        return 'border-yellow-500';
      case TaskPriority.LOW:
        return 'border-green-500';
      default:
        return 'border-gray-300';
    }
  }

  getPriorityBadgeClass(): string {
    switch (this.task.priority) {
      case TaskPriority.URGENT:
        return 'bg-red-100 text-red-800';
      case TaskPriority.HIGH:
        return 'bg-orange-100 text-orange-800';
      case TaskPriority.MEDIUM:
        return 'bg-yellow-100 text-yellow-800';
      case TaskPriority.LOW:
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  getPriorityLabel(): string {
    switch (this.task.priority) {
      case TaskPriority.URGENT:
        return 'Срочно';
      case TaskPriority.HIGH:
        return 'Высокий';
      case TaskPriority.MEDIUM:
        return 'Средний';
      case TaskPriority.LOW:
        return 'Низкий';
      default:
        return '';
    }
  }

  isOverdue(): boolean {
    if (!this.task.deadline) return false;
    return new Date(this.task.deadline) < new Date() && this.task.status !== TaskStatus.COMPLETED;
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', {
      day: 'numeric',
      month: 'short',
    });
  }
}

