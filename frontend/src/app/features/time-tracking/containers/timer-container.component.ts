import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { TimerDisplayComponent } from '../components/timer-display.component';
import { TimerFacadeService } from '../services/timer-facade.service';
import { TimerUpdate } from '../../../shared/models/time-entry.model';
import { AuthUtilsService } from '../../../core/auth/auth-utils.service';

/**
 * Timer Container Component (Smart).
 *
 * Container компонент для управления таймером.
 * Отвечает за:
 * - Подписку на WebSocket обновления таймера
 * - Отображение floating timer display
 * - Остановку таймера
 * - Навигацию к деталям задачи
 *
 * WebSocket подключается к /ws/timer?userId={userId}
 */
@Component({
  selector: 'app-timer-container',
  standalone: true,
  imports: [CommonModule, TimerDisplayComponent],
  template: `
    <app-timer-display
      [isRunning]="(timerViewModel$ | async)?.isRunning || false"
      [elapsedSeconds]="(timerViewModel$ | async)?.elapsedSeconds || 0"
      [taskTitle]="(timerViewModel$ | async)?.taskTitle || ''"
      [projectName]="(timerViewModel$ | async)?.projectName || ''"
      [projectColor]="(timerViewModel$ | async)?.projectColor || '#3b82f6'"
      [currentCost]="formatCost((timerUpdate$ | async)?.elapsedSeconds || 0)"
      (stopTimer)="onStopTimer()"
      (viewDetails)="onViewTaskDetails()">
    </app-timer-display>
  `,
  styles: [
    `
      :host {
        display: contents;
      }
    `,
  ],
})
export class TimerContainerComponent implements OnInit, OnDestroy {
  private timerFacade = inject(TimerFacadeService);
  private router = inject(Router);
  private authUtils = inject(AuthUtilsService);

  private destroy$ = new Subject<void>();
  private webSocket: WebSocket | null = null;

  // Observables from facade
  timerViewModel$ = this.timerFacade.timerViewModel$;
  timerUpdate$ = this.timerFacade.timerUpdate$;
  activeTimer$ = this.timerFacade.activeTimer$;

  ngOnInit(): void {
    // Проверяем аутентификацию перед загрузкой таймера и WebSocket
    this.authUtils.isAuthenticated().pipe(takeUntil(this.destroy$)).subscribe((isAuth) => {
      if (isAuth) {
        // Load active timer on init (only if authenticated)
        this.timerFacade.loadActiveTimer();

        // Setup WebSocket connection
        this.setupWebSocket();

        // Subscribe to timer updates from store
        this.timerUpdate$.pipe(takeUntil(this.destroy$)).subscribe((update) => {
          if (update) {
            console.debug('Timer update received:', update);
          }
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.closeWebSocket();
  }

  /**
   * Настраивает WebSocket соединение для получения real-time обновлений таймера.
   *
   * Извлекает userId из AuthUtilsService и подключается к /ws/timer?userId={userId}.
   */
  private setupWebSocket(): void {
    // Получаем userId синхронно для WebSocket setup
    const userId = this.authUtils.getCurrentUserIdSync();

    if (!userId) {
      console.warn('User ID not available for WebSocket connection. User may not be authenticated.');
      return;
    }

    try {
      // Construct WebSocket URL with userId query parameter
      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsHost = window.location.hostname;
      const wsPort = window.location.port ? `:${window.location.port}` : '';
      const wsUrl = `${wsProtocol}//${wsHost}${wsPort}/ws/timer?userId=${userId}`;

      console.log('Connecting to WebSocket:', wsUrl);

      this.webSocket = new WebSocket(wsUrl);

      this.webSocket.onopen = () => {
        console.log('WebSocket connection established for timer updates');
      };

      this.webSocket.onmessage = (event) => {
        try {
          const timerUpdate: TimerUpdate = JSON.parse(event.data);
          console.debug('WebSocket timer update:', timerUpdate);

          // Dispatch update to store через facade
          this.timerFacade.handleTimerUpdate(timerUpdate);
        } catch (error) {
          console.error('Failed to parse WebSocket message:', error);
        }
      };

      this.webSocket.onerror = (error) => {
        console.error('WebSocket error:', error);
      };

      this.webSocket.onclose = (event) => {
        console.log('WebSocket connection closed:', event.code, event.reason);

        // Reconnect after 5 seconds if connection was lost unexpectedly
        if (event.code !== 1000) {
          console.log('Attempting to reconnect in 5 seconds...');
          setTimeout(() => {
            if (!this.destroy$.closed) {
              this.setupWebSocket();
            }
          }, 5000);
        }
      };
    } catch (error) {
      console.error('Failed to setup WebSocket:', error);
    }
  }

  /**
   * Закрывает WebSocket соединение.
   */
  private closeWebSocket(): void {
    if (this.webSocket) {
      this.webSocket.close(1000, 'Component destroyed');
      this.webSocket = null;
    }
  }

  /**
   * Останавливает активный таймер.
   */
  onStopTimer(): void {
    this.timerFacade.stopTimer();
  }

  /**
   * Переходит к деталям задачи, на которой запущен таймер.
   */
  onViewTaskDetails(): void {
    this.timerUpdate$.pipe(takeUntil(this.destroy$)).subscribe((update) => {
      if (update?.taskId) {
        this.router.navigate(['/tasks', update.taskId]);
      }
    });
  }

  /**
   * Форматирует стоимость для отображения.
   *
   * TODO: Получать hourly rate из TimeEntry или User для расчета cost.
   */
  formatCost(elapsedSeconds: number): string {
    // Mock hourly rate for now
    const hourlyRate = 1000; // руб/час
    const hours = elapsedSeconds / 3600;
    const cost = hours * hourlyRate;

    if (cost < 1) {
      return '';
    }

    return `${cost.toFixed(0)} ₽`;
  }

}

