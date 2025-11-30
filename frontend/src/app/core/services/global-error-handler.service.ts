import { ErrorHandler, Injectable, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

/**
 * Global Error Handler Service.
 *
 * Централизованная обработка ошибок приложения:
 * - HTTP ошибки (401, 403, 404, 500, etc.)
 * - JavaScript runtime errors
 * - Unhandled promise rejections
 * - Network errors
 *
 * Логирует ошибки в консоль (в production можно отправлять в external service).
 * Показывает user-friendly уведомления.
 * Автоматическая обработка authentication errors (401).
 */
@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private router = inject(Router);

  /**
   * Обрабатывает все необработанные ошибки в приложении.
   *
   * @param error любая ошибка (Error, HttpErrorResponse, etc.)
   */
  handleError(error: Error | HttpErrorResponse): void {
    // Логируем ошибку в консоль (в production отправлять в Sentry/LogRocket)
    console.error('Global error caught:', error);

    if (error instanceof HttpErrorResponse) {
      // HTTP ошибка от backend
      this.handleHttpError(error);
    } else {
      // JavaScript runtime error
      this.handleClientError(error);
    }
  }

  /**
   * Обрабатывает HTTP ошибки от backend API.
   *
   * @param error HTTP error response
   */
  private handleHttpError(error: HttpErrorResponse): void {
    const status = error.status;
    const message = this.getHttpErrorMessage(error);

    console.error(`HTTP Error ${status}:`, message);

    switch (status) {
      case 0:
        // Network error или CORS issue
        this.showNotification('Ошибка сети', 'Не удалось подключиться к серверу. Проверьте интернет-соединение.');
        break;

      case 400:
        // Bad Request
        this.showNotification('Неверный запрос', message || 'Проверьте введенные данные.');
        break;

      case 401:
        // Unauthorized - redirect to login
        console.warn('Unauthorized access - redirecting to login');
        this.showNotification('Требуется авторизация', 'Пожалуйста, войдите в систему.');
        this.router.navigate(['/auth/login'], {
          queryParams: { returnUrl: this.router.url },
        });
        break;

      case 403:
        // Forbidden
        this.showNotification('Доступ запрещен', 'У вас нет прав для выполнения этого действия.');
        break;

      case 404:
        // Not Found
        this.showNotification('Не найдено', message || 'Запрошенный ресурс не найден.');
        break;

      case 409:
        // Conflict
        this.showNotification('Конфликт', message || 'Операция не может быть выполнена из-за конфликта данных.');
        break;

      case 422:
        // Unprocessable Entity (validation error)
        this.showNotification('Ошибка валидации', message || 'Проверьте правильность введенных данных.');
        break;

      case 429:
        // Too Many Requests
        this.showNotification('Слишком много запросов', 'Подождите немного перед следующей попыткой.');
        break;

      case 500:
      case 502:
      case 503:
      case 504:
        // Server errors
        this.showNotification('Ошибка сервера', 'Произошла ошибка на сервере. Попробуйте позже.');
        break;

      default:
        this.showNotification('Ошибка', message || `Произошла ошибка (${status})`);
    }
  }

  /**
   * Обрабатывает JavaScript runtime ошибки.
   *
   * @param error JavaScript Error
   */
  private handleClientError(error: Error): void {
    console.error('Client-side error:', error.message);
    console.error('Stack trace:', error.stack);

    // В development показываем подробную информацию
    if (!this.isProduction()) {
      this.showNotification('JavaScript Error', error.message);
    } else {
      // В production показываем generic сообщение
      this.showNotification('Ошибка приложения', 'Произошла непредвиденная ошибка. Попробуйте обновить страницу.');
    }

    // TODO: В production отправлять в error tracking service (Sentry, Rollbar, etc.)
    // this.sendToErrorTracker(error);
  }

  /**
   * Извлекает human-readable сообщение из HTTP error response.
   *
   * @param error HTTP error response
   * @returns error message string
   */
  private getHttpErrorMessage(error: HttpErrorResponse): string {
    // Пытаемся извлечь сообщение из backend error response
    if (error.error) {
      // Стандартный backend error format: { message: string }
      if (typeof error.error === 'string') {
        return error.error;
      }

      if (error.error.message) {
        return error.error.message;
      }

      if (error.error.error) {
        return error.error.error;
      }

      // Spring Boot error format
      if (error.error.error_description) {
        return error.error.error_description;
      }
    }

    // Fallback to status text
    return error.statusText || 'Unknown error';
  }

  /**
   * Показывает уведомление пользователю.
   *
   * Временная реализация с console.error.
   * TODO: Интегрировать с notification service (toast, snackbar, etc.)
   *
   * @param title заголовок уведомления
   * @param message сообщение уведомления
   */
  private showNotification(title: string, message: string): void {
    // Временно показываем в консоли
    console.error(`[${title}]`, message);

    // TODO: Показать toast notification через NotificationService
    // this.notificationService.showError(title, message);

    // Можно также показать browser alert для критичных ошибок
    // if (this.isCriticalError(title)) {
    //   alert(`${title}: ${message}`);
    // }
  }

  /**
   * Проверяет, запущено ли приложение в production режиме.
   *
   * @returns true если production
   */
  private isProduction(): boolean {
    // Проверяем environment.production
    // В runtime можно проверить через window.location.hostname
    return !window.location.hostname.includes('localhost');
  }

  /**
   * Отправляет ошибку в external error tracking service.
   *
   * TODO: Интегрировать с Sentry, Rollbar, LogRocket, или другим сервисом.
   *
   * @param error ошибка для отправки
   */
  private sendToErrorTracker(error: Error): void {
    // Example: Sentry integration
    // Sentry.captureException(error);

    // Example: Custom API endpoint
    // this.http.post('/api/errors', {
    //   message: error.message,
    //   stack: error.stack,
    //   timestamp: new Date().toISOString(),
    //   userAgent: navigator.userAgent,
    //   url: window.location.href
    // }).subscribe();
  }
}



