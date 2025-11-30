import { Injectable, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable, map } from 'rxjs';
import { selectCurrentUser, selectUserId } from '../../features/auth/store/auth.selectors';

/**
 * Auth Utilities Service.
 *
 * Предоставляет вспомогательные методы для работы с аутентификацией:
 * - Извлечение userId из store
 * - Декодирование JWT токенов
 * - Проверка аутентификации
 */
@Injectable({
  providedIn: 'root',
})
export class AuthUtilsService {
  private store = inject(Store);

  /**
   * Получает ID текущего пользователя из store.
   *
   * @returns Observable<string | null> userId или null если пользователь не аутентифицирован
   */
  getCurrentUserId(): Observable<string | null> {
    return this.store.select(selectUserId).pipe(map((id) => id || null));
  }

  /**
   * Получает ID текущего пользователя синхронно из localStorage.
   *
   * Используется в случаях, когда Observable недоступен (WebSocket setup).
   *
   * @returns string | null userId или null если пользователь не аутентифицирован
   */
  getCurrentUserIdSync(): string | null {
    try {
      // Пытаемся получить userId из user object в localStorage
      const userJson = localStorage.getItem('user');
      if (userJson && userJson !== 'undefined' && userJson !== 'null') {
        const user = JSON.parse(userJson);
        return user?.id || null;
      }

      // Fallback: пытаемся декодировать JWT токен
      const accessToken = localStorage.getItem('access_token');
      if (accessToken && accessToken !== 'undefined' && accessToken !== 'null') {
        return this.extractUserIdFromToken(accessToken);
      }

      return null;
    } catch (error) {
      console.error('Failed to get current user ID:', error);
      return null;
    }
  }

  /**
   * Извлекает userId из JWT токена.
   *
   * JWT payload содержит userId в claim 'sub' (subject).
   *
   * @param token JWT токен
   * @returns string | null userId или null если токен невалиден
   */
  private extractUserIdFromToken(token: string): string | null {
    try {
      // JWT состоит из 3 частей, разделенных точками: header.payload.signature
      const parts = token.split('.');
      if (parts.length !== 3) {
        console.warn('Invalid JWT token format');
        return null;
      }

      // Декодируем payload (вторая часть)
      const payload = parts[1];
      const decodedPayload = this.base64UrlDecode(payload);
      const payloadObj = JSON.parse(decodedPayload);

      // Стандартный JWT claim для user ID - 'sub' (subject)
      // Также проверяем custom claim 'userId' на случай если backend использует его
      return payloadObj.sub || payloadObj.userId || null;
    } catch (error) {
      console.error('Failed to extract user ID from token:', error);
      return null;
    }
  }

  /**
   * Декодирует base64url строку в UTF-8.
   *
   * JWT использует base64url encoding (не стандартный base64).
   *
   * @param str base64url encoded string
   * @returns decoded string
   */
  private base64UrlDecode(str: string): string {
    // Заменяем base64url символы на стандартные base64
    let base64 = str.replace(/-/g, '+').replace(/_/g, '/');

    // Добавляем padding если нужно
    while (base64.length % 4 !== 0) {
      base64 += '=';
    }

    // Декодируем base64 в UTF-8
    try {
      return decodeURIComponent(
        atob(base64)
          .split('')
          .map((c) => {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
          })
          .join('')
      );
    } catch (error) {
      console.error('Failed to decode base64url:', error);
      return '';
    }
  }

  /**
   * Проверяет, аутентифицирован ли текущий пользователь.
   *
   * @returns Observable<boolean>
   */
  isAuthenticated(): Observable<boolean> {
    return this.store.select(selectCurrentUser).pipe(map((user) => user !== null));
  }

  /**
   * Получает email текущего пользователя.
   *
   * @returns Observable<string | null>
   */
  getCurrentUserEmail(): Observable<string | null> {
    return this.store.select(selectCurrentUser).pipe(map((user) => user?.email || null));
  }

  /**
   * Получает имя текущего пользователя.
   *
   * @returns Observable<string | null>
   */
  getCurrentUserName(): Observable<string | null> {
    return this.store.select(selectCurrentUser).pipe(map((user) => user?.name || null));
  }
}

