import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IdleTimerService } from '../../core/services/idle-timer.service';

/**
 * Dialog component that warns user about inactivity (FR-007.3).
 *
 * Shows countdown timer and allows user to extend session or logout.
 */
@Component({
  selector: 'app-idle-warning-dialog',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="overlay" *ngIf="show" (click)="onOverlayClick($event)">
      <div class="dialog" (click)="$event.stopPropagation()">
        <div class="icon-warning">⏰</div>
        <h2>Сессия скоро завершится</h2>
        <p class="message">
          Вы неактивны. Сессия автоматически завершится через:
        </p>
        <div class="countdown">{{ formattedTime }}</div>
        <p class="hint">
          Нажмите "Продолжить работу" для продления сессии или "Выйти" для завершения работы.
        </p>
        <div class="actions">
          <button class="btn-secondary" (click)="onLogout()">
            Выйти
          </button>
          <button class="btn-primary" (click)="onContinue()">
            Продолжить работу
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.75);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
        animation: fadeIn 0.2s ease-in;
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }

      .dialog {
        background: white;
        border-radius: 16px;
        padding: 40px;
        max-width: 450px;
        width: 90%;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
        text-align: center;
        animation: slideUp 0.3s ease-out;
      }

      @keyframes slideUp {
        from {
          transform: translateY(20px);
          opacity: 0;
        }
        to {
          transform: translateY(0);
          opacity: 1;
        }
      }

      .icon-warning {
        font-size: 64px;
        margin-bottom: 20px;
        animation: pulse 2s ease-in-out infinite;
      }

      @keyframes pulse {
        0%, 100% {
          transform: scale(1);
        }
        50% {
          transform: scale(1.1);
        }
      }

      h2 {
        font-size: 24px;
        font-weight: 700;
        color: #1f2937;
        margin-bottom: 16px;
      }

      .message {
        color: #6b7280;
        font-size: 15px;
        margin-bottom: 24px;
        line-height: 1.5;
      }

      .countdown {
        font-size: 48px;
        font-weight: 700;
        color: #ef4444;
        font-family: 'Courier New', monospace;
        margin-bottom: 24px;
        padding: 20px;
        background: #fef2f2;
        border-radius: 12px;
        border: 2px solid #fecaca;
      }

      .hint {
        color: #9ca3af;
        font-size: 13px;
        margin-bottom: 30px;
      }

      .actions {
        display: flex;
        gap: 12px;
        justify-content: center;
      }

      .btn-primary,
      .btn-secondary {
        padding: 12px 24px;
        border: none;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.2s;
        min-width: 140px;
      }

      .btn-primary {
        background: #667eea;
        color: white;
      }

      .btn-primary:hover {
        background: #5568d3;
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
      }

      .btn-secondary {
        background: #f3f4f6;
        color: #6b7280;
      }

      .btn-secondary:hover {
        background: #e5e7eb;
        color: #374151;
      }

      @media (max-width: 480px) {
        .dialog {
          padding: 30px 20px;
        }

        .actions {
          flex-direction: column;
        }

        .btn-primary,
        .btn-secondary {
          width: 100%;
        }
      }
    `,
  ],
})
export class IdleWarningDialogComponent {
  @Input() show = false;
  @Input() secondsRemaining = 0;

  @Output() continueSession = new EventEmitter<void>();
  @Output() logout = new EventEmitter<void>();

  get formattedTime(): string {
    return IdleTimerService.formatTime(this.secondsRemaining);
  }

  onContinue(): void {
    this.continueSession.emit();
  }

  onLogout(): void {
    this.logout.emit();
  }

  onOverlayClick(event: Event): void {
    // Close dialog on overlay click (same as continue)
    this.onContinue();
  }
}

