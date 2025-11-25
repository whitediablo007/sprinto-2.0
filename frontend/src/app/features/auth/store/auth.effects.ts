import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { catchError, map, switchMap, tap } from 'rxjs/operators';
import { AuthService } from '../../../core/auth/auth.service';
import * as AuthActions from './auth.actions';

/**
 * Auth Effects.
 * 
 * Handles side effects for authentication actions including:
 * - API calls for login/register/refresh
 * - Token storage in localStorage
 * - Navigation after successful auth
 */
@Injectable()
export class AuthEffects {
  private readonly STORAGE_KEY_ACCESS_TOKEN = 'access_token';
  private readonly STORAGE_KEY_REFRESH_TOKEN = 'refresh_token';
  private readonly STORAGE_KEY_USER = 'user';

  /**
   * Login effect.
   */
  login$;

  /**
   * Register effect.
   */
  register$;

  /**
   * Refresh token effect.
   */
  refreshToken$;

  /**
   * Store tokens and user in localStorage after successful login/register.
   */
  storeAuthData$;

  /**
   * Navigate to dashboard after successful login/register.
   */
  navigateToDashboard$;

  /**
   * Clear localStorage and navigate to login on logout.
   */
  logout$;

  /**
   * Load user data from localStorage on app init.
   */
  loadUserFromStorage$;

  constructor(
    private actions$: Actions,
    private authService: AuthService,
    private router: Router
  ) {
    this.login$ = createEffect(() =>
      this.actions$.pipe(
        ofType(AuthActions.login),
        switchMap(({ credentials }) =>
          this.authService.login(credentials).pipe(
            map(response => AuthActions.loginSuccess({ response })),
            catchError(error => of(AuthActions.loginFailure({ 
              error: error.error?.message || 'Login failed' 
            })))
          )
        )
      )
    );

    this.register$ = createEffect(() =>
      this.actions$.pipe(
        ofType(AuthActions.register),
        switchMap(({ userData }) =>
          this.authService.register(userData).pipe(
            map(response => AuthActions.registerSuccess({ response })),
            catchError(error => of(AuthActions.registerFailure({ 
              error: error.error?.message || 'Registration failed' 
            })))
          )
        )
      )
    );

    this.refreshToken$ = createEffect(() =>
      this.actions$.pipe(
        ofType(AuthActions.refreshToken),
        switchMap(({ refreshToken }) =>
          this.authService.refreshToken(refreshToken).pipe(
            map(response => AuthActions.refreshTokenSuccess({ response })),
            catchError(error => of(AuthActions.refreshTokenFailure({ 
              error: error.error?.message || 'Token refresh failed' 
            })))
          )
        )
      )
    );

    this.storeAuthData$ = createEffect(
      () =>
        this.actions$.pipe(
          ofType(
            AuthActions.loginSuccess,
            AuthActions.registerSuccess,
            AuthActions.refreshTokenSuccess
          ),
          tap(({ response }) => {
            localStorage.setItem(this.STORAGE_KEY_ACCESS_TOKEN, response.accessToken);
            localStorage.setItem(this.STORAGE_KEY_REFRESH_TOKEN, response.refreshToken);
            localStorage.setItem(this.STORAGE_KEY_USER, JSON.stringify(response.user));
          })
        ),
      { dispatch: false }
    );

    this.navigateToDashboard$ = createEffect(
      () =>
        this.actions$.pipe(
          ofType(AuthActions.loginSuccess, AuthActions.registerSuccess),
          tap(() => {
            this.router.navigate(['/dashboard']);
          })
        ),
      { dispatch: false }
    );

    this.logout$ = createEffect(
      () =>
        this.actions$.pipe(
          ofType(AuthActions.logout),
          tap(() => {
            localStorage.removeItem(this.STORAGE_KEY_ACCESS_TOKEN);
            localStorage.removeItem(this.STORAGE_KEY_REFRESH_TOKEN);
            localStorage.removeItem(this.STORAGE_KEY_USER);
            this.router.navigate(['/auth/login']);
          })
        ),
      { dispatch: false }
    );

    this.loadUserFromStorage$ = createEffect(() =>
      this.actions$.pipe(
        ofType(AuthActions.loadUserFromStorage),
        map(() => {
          const accessToken = localStorage.getItem(this.STORAGE_KEY_ACCESS_TOKEN);
          const refreshToken = localStorage.getItem(this.STORAGE_KEY_REFRESH_TOKEN);
          const userJson = localStorage.getItem(this.STORAGE_KEY_USER);

          if (accessToken && refreshToken && userJson) {
            const user = JSON.parse(userJson);
            return AuthActions.loadUserFromStorageSuccess({
              accessToken,
              refreshToken,
              user
            });
          }

          return AuthActions.loadUserFromStorageFailure();
        })
      )
    );
  }
}
