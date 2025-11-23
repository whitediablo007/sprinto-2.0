import { ApplicationConfig, provideZoneChangeDetection, isDevMode } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideStore } from '@ngrx/store';
import { provideStoreDevtools } from '@ngrx/store-devtools';
import { provideEffects } from '@ngrx/effects';
import { provideAnimations } from '@angular/platform-browser/animations';
import { DialogService } from 'primeng/dynamicdialog';
import { routes } from './app.routes';
import { rootReducers } from './store/root-state';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthEffects } from './features/auth/store/auth.effects';

/**
 * Root application configuration.
 * 
 * Provides all necessary providers for the application including:
 * - Routing
 * - HTTP client with interceptors
 * - NgRx store with DevTools
 * - Animations
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([authInterceptor])
    ),
    provideStore(rootReducers),
    provideEffects([AuthEffects]),
    provideStoreDevtools({
      maxAge: 25,
      logOnly: !isDevMode(),
      trace: false,
      traceLimit: 75
    }),
    provideAnimations(),
    DialogService
  ]
};

