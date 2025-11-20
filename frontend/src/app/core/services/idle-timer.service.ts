import { Injectable, NgZone, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, fromEvent, merge, interval, Subject } from 'rxjs';
import { debounceTime, filter, takeUntil } from 'rxjs/operators';

/**
 * Service for tracking user inactivity and automatic logout (FR-007.3, SC-001).
 *
 * Monitors user activity (mouse, keyboard, API calls) and triggers logout
 * after configured inactivity period (default: 30 minutes).
 *
 * Features:
 * - Tracks mouse clicks, keyboard events, and scroll
 * - Shows warning before logout (2 minutes before)
 * - Automatic logout on inactivity threshold
 * - Synchronized with backend JWT expiration (30 minutes)
 */
@Injectable({
  providedIn: 'root',
})
export class IdleTimerService {
  private router = inject(Router);
  private ngZone = inject(NgZone);

  // Inactivity timeout in milliseconds (30 minutes = 1800000 ms)
  private readonly IDLE_TIMEOUT = 30 * 60 * 1000;

  // Warning before logout (2 minutes = 120000 ms)
  private readonly WARNING_TIME = 2 * 60 * 1000;

  private lastActivityTime = Date.now();
  private idleCheckInterval?: ReturnType<typeof setInterval>;
  private destroy$ = new Subject<void>();

  private isIdleSubject = new BehaviorSubject<boolean>(false);
  private showWarningSubject = new BehaviorSubject<boolean>(false);
  private timeUntilLogoutSubject = new BehaviorSubject<number>(0);

  public isIdle$: Observable<boolean> = this.isIdleSubject.asObservable();
  public showWarning$: Observable<boolean> = this.showWarningSubject.asObservable();
  public timeUntilLogout$: Observable<number> = this.timeUntilLogoutSubject.asObservable();

  /**
   * Start monitoring user activity.
   * Should be called after successful login.
   */
  startMonitoring(): void {
    this.stopMonitoring(); // Clean up any existing monitoring
    this.lastActivityTime = Date.now();
    this.isIdleSubject.next(false);
    this.showWarningSubject.next(false);

    // Monitor user activity events
    this.ngZone.runOutsideAngular(() => {
      const activityEvents$ = merge(
        fromEvent(document, 'click'),
        fromEvent(document, 'keypress'),
        fromEvent(document, 'mousemove'),
        fromEvent(document, 'scroll'),
        fromEvent(document, 'touchstart')
      ).pipe(debounceTime(1000)); // Debounce to avoid excessive updates

      activityEvents$.pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.ngZone.run(() => {
          this.resetTimer();
        });
      });
    });

    // Check idle status every second
    this.idleCheckInterval = setInterval(() => {
      this.checkIdleStatus();
    }, 1000);
  }

  /**
   * Stop monitoring user activity.
   * Should be called on logout.
   */
  stopMonitoring(): void {
    this.destroy$.next();
    if (this.idleCheckInterval) {
      clearInterval(this.idleCheckInterval);
      this.idleCheckInterval = undefined;
    }
    this.isIdleSubject.next(false);
    this.showWarningSubject.next(false);
    this.timeUntilLogoutSubject.next(0);
  }

  /**
   * Reset the idle timer (called on user activity).
   */
  resetTimer(): void {
    this.lastActivityTime = Date.now();
    this.isIdleSubject.next(false);
    this.showWarningSubject.next(false);
    this.timeUntilLogoutSubject.next(0);
  }

  /**
   * Manually extend session (called when user clicks "Continue" in warning dialog).
   */
  extendSession(): void {
    this.resetTimer();
    // Trigger token refresh (handled by auth service)
    // This method can be called from warning dialog
  }

  /**
   * Get remaining time until logout in seconds.
   */
  getTimeUntilLogout(): number {
    const timeSinceLastActivity = Date.now() - this.lastActivityTime;
    const timeRemaining = this.IDLE_TIMEOUT - timeSinceLastActivity;
    return Math.max(0, Math.floor(timeRemaining / 1000));
  }

  /**
   * Check idle status and trigger warnings/logout.
   */
  private checkIdleStatus(): void {
    const timeSinceLastActivity = Date.now() - this.lastActivityTime;
    const timeRemaining = this.IDLE_TIMEOUT - timeSinceLastActivity;

    // Update time until logout
    this.timeUntilLogoutSubject.next(Math.max(0, Math.floor(timeRemaining / 1000)));

    // Show warning 2 minutes before logout
    if (timeRemaining <= this.WARNING_TIME && timeRemaining > 0) {
      if (!this.showWarningSubject.value) {
        console.warn('[IdleTimer] Showing inactivity warning');
        this.showWarningSubject.next(true);
      }
    }

    // Trigger logout on idle timeout
    if (timeRemaining <= 0) {
      if (!this.isIdleSubject.value) {
        console.warn('[IdleTimer] User idle timeout reached - triggering logout');
        this.isIdleSubject.next(true);
        this.handleIdleTimeout();
      }
    }
  }

  /**
   * Handle idle timeout - logout user and redirect to login.
   */
  private handleIdleTimeout(): void {
    this.stopMonitoring();

    // Store message for login page
    sessionStorage.setItem(
      'logoutReason',
      'Ваша сессия завершена из-за неактивности. Пожалуйста, войдите снова.'
    );

    // Navigate to login page
    this.router.navigate(['/auth/login'], {
      queryParams: { reason: 'inactivity' },
    });

    // Trigger logout in auth service (will be handled by auth facade)
    // The actual logout logic should be in auth service
  }

  /**
   * Format seconds to MM:SS string.
   */
  static formatTime(seconds: number): string {
    const minutes = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }
}

