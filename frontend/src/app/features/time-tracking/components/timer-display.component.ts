import { Component, ChangeDetectionStrategy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Timer Display Component (Presentational).
 *
 * Отображает floating button с активным таймером.
 * Показывает elapsed time, task title и кнопку Stop.
 *
 * Использует OnPush change detection для максимальной производительности.
 */
@Component({
  selector: 'app-timer-display',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      *ngIf="isRunning"
      class="fixed bottom-6 right-6 bg-white rounded-lg shadow-lg p-4 border-2 border-blue-500 min-w-[300px] z-50"
      [attr.aria-live]="'polite'">
      <!-- Project color indicator -->
      <div class="h-1 w-full rounded-t-lg mb-3" [style.backgroundColor]="projectColor || '#3b82f6'"></div>

      <!-- Task title -->
      <div class="text-sm font-medium text-gray-900 mb-2 truncate" [title]="taskTitle">
        {{ taskTitle || 'Без названия' }}
      </div>

      <!-- Project name (optional) -->
      <div *ngIf="projectName" class="text-xs text-gray-500 mb-3">
        {{ projectName }}
      </div>

      <!-- Timer display -->
      <div class="flex items-center justify-between mb-3">
        <div class="text-2xl font-mono font-bold text-blue-600">
          {{ formattedTime }}
        </div>

        <div *ngIf="currentCost" class="text-sm text-gray-600">{{ currentCost }}</div>
      </div>

      <!-- Control buttons -->
      <div class="flex gap-2">
        <button
          type="button"
          (click)="onStop()"
          class="flex-1 px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2"
          [attr.aria-label]="'Stop timer for ' + taskTitle">
          <span class="flex items-center justify-center gap-2">
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <rect x="6" y="6" width="8" height="8" />
            </svg>
            Остановить
          </span>
        </button>

        <button
          *ngIf="showDetailsButton"
          type="button"
          (click)="onViewDetails()"
          class="px-4 py-2 bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors focus:outline-none focus:ring-2 focus:ring-gray-400 focus:ring-offset-2"
          [attr.aria-label]="'View task details'">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </button>
      </div>

      <!-- Animated pulse indicator -->
      <div class="absolute top-2 left-2 flex items-center">
        <span class="relative flex h-3 w-3">
          <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
          <span class="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
        </span>
      </div>
    </div>
  `,
  styles: [
    `
      :host {
        display: contents;
      }

      .animate-ping {
        animation: ping 1s cubic-bezier(0, 0, 0.2, 1) infinite;
      }

      @keyframes ping {
        75%,
        100% {
          transform: scale(2);
          opacity: 0;
        }
      }
    `,
  ],
})
export class TimerDisplayComponent {
  @Input({ required: true }) isRunning!: boolean;
  @Input() elapsedSeconds: number = 0;
  @Input() taskTitle: string = '';
  @Input() projectName: string = '';
  @Input() projectColor: string = '#3b82f6';
  @Input() currentCost: string = '';
  @Input() showDetailsButton: boolean = true;

  @Output() stopTimer = new EventEmitter<void>();
  @Output() viewDetails = new EventEmitter<void>();

  /**
   * Форматирует elapsed seconds в HH:MM:SS.
   */
  get formattedTime(): string {
    const hours = Math.floor(this.elapsedSeconds / 3600);
    const minutes = Math.floor((this.elapsedSeconds % 3600) / 60);
    const seconds = this.elapsedSeconds % 60;

    return [hours, minutes, seconds].map((val) => val.toString().padStart(2, '0')).join(':');
  }

  onStop(): void {
    this.stopTimer.emit();
  }

  onViewDetails(): void {
    this.viewDetails.emit();
  }
}

