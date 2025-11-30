import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { exhaustMap, take } from 'rxjs/operators';
import { selectAccessToken } from '../../features/auth/store/auth.selectors';

/**
 * HTTP Interceptor for JWT token authentication.
 * 
 * Automatically attaches JWT access token to outgoing HTTP requests
 * and handles Correlation ID for request tracing (NFR-038 to NFR-043).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(Store);

  // Skip authentication for auth endpoints
  if (req.url.includes('/auth/login') || 
      req.url.includes('/auth/register') ||
      req.url.includes('/auth/password-reset')) {
    return next(addCorrelationId(req));
  }

  // Get access token from store and attach to request
  return store.select(selectAccessToken).pipe(
    take(1),
    exhaustMap(storeToken => {
      // Используем токен из store, или fallback на localStorage
      // (store может не успеть обновиться при первой загрузке)
      const token = storeToken || getTokenFromStorage();
      
      let authReq = req;
      
      if (token) {
        authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
      }
      
      return next(addCorrelationId(authReq));
    })
  );
};

/**
 * Fallback: получает токен напрямую из localStorage.
 * Используется когда NgRx store ещё не успел обновиться.
 */
function getTokenFromStorage(): string | null {
  try {
    const token = localStorage.getItem('access_token');
    if (token && token !== 'undefined' && token !== 'null') {
      return token;
    }
    return null;
  } catch {
    return null;
  }
}

/**
 * Adds Correlation ID header to request for distributed tracing.
 * If request already has X-Correlation-ID, it's preserved.
 * Otherwise, generates a new UUID.
 */
function addCorrelationId(req: any): any {
  if (req.headers.has('X-Correlation-ID')) {
    return req;
  }
  
  const correlationId = crypto.randomUUID();
  return req.clone({
    setHeaders: {
      'X-Correlation-ID': correlationId
    }
  });
}

