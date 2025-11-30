import { Component, ChangeDetectionStrategy, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskStatus } from '../models/task.model';

/**
 * Task Status Badge Component (Presentational).
 *
 * Отображает статус задачи в виде цветного badge.
 * Использует OnPush change detection для максимальной производительности.
 */
@Component({
  selector: 'app-task-status-badge',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="px-3 py-1 rounded-full text-sm font-medium"
      [ngClass]="getBadgeClasses()"
      [attr.aria-label]="'Task status: ' + getStatusLabel()">
      {{ getStatusLabel() }}
    </span>
  `,
  styles: [
    `
      :host {
        display: inline-block;
      }
    `,
  ],
})
export class TaskStatusBadgeComponent {
  @Input({ required: true }) status!: TaskStatus;

  /**
   * Получает CSS классы для badge на основе статуса.
   */
  getBadgeClasses(): string {
    const baseClasses = '';

    switch (this.status) {
      case TaskStatus.NEW:
        return 'bg-gray-100 text-gray-800';

      case TaskStatus.IN_PROGRESS:
        return 'bg-blue-100 text-blue-800';

      case TaskStatus.TESTING:
        return 'bg-purple-100 text-purple-800';

      case TaskStatus.BLOCKED:
        return 'bg-red-100 text-red-800';

      case TaskStatus.COMPLETED:
        return 'bg-green-100 text-green-800';

      case TaskStatus.CANCELLED:
        return 'bg-gray-200 text-gray-600';

      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  /**
   * Получает человеко-читаемую метку для статуса.
   */
  getStatusLabel(): string {
    switch (this.status) {
      case TaskStatus.NEW:
        return 'Новая';

      case TaskStatus.IN_PROGRESS:
        return 'В работе';

      case TaskStatus.TESTING:
        return 'Тестирование';

      case TaskStatus.BLOCKED:
        return 'Заблокирована';

      case TaskStatus.COMPLETED:
        return 'Завершена';

      case TaskStatus.CANCELLED:
        return 'Отменена';

      default:
        return 'Неизвестно';
    }
  }
}

