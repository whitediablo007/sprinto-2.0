import { createAction, props } from '@ngrx/store';
import { AuthResponse, LoginRequest, RegisterRequest, User } from '../../../core/auth/auth.service';

/**
 * Auth Actions for NgRx Store.
 * 
 * Defines all authentication-related actions including:
 * - Login/Register
 * - Token refresh
 * - Logout
 * - Load user from storage
 */

// Login Actions
export const login = createAction(
  '[Auth] Login',
  props<{ credentials: LoginRequest }>()
);

export const loginSuccess = createAction(
  '[Auth] Login Success',
  props<{ response: AuthResponse }>()
);

export const loginFailure = createAction(
  '[Auth] Login Failure',
  props<{ error: string }>()
);

// Register Actions
export const register = createAction(
  '[Auth] Register',
  props<{ userData: RegisterRequest }>()
);

export const registerSuccess = createAction(
  '[Auth] Register Success',
  props<{ response: AuthResponse }>()
);

export const registerFailure = createAction(
  '[Auth] Register Failure',
  props<{ error: string }>()
);

// Refresh Token Actions
export const refreshToken = createAction(
  '[Auth] Refresh Token',
  props<{ refreshToken: string }>()
);

export const refreshTokenSuccess = createAction(
  '[Auth] Refresh Token Success',
  props<{ response: AuthResponse }>()
);

export const refreshTokenFailure = createAction(
  '[Auth] Refresh Token Failure',
  props<{ error: string }>()
);

// Logout Action
export const logout = createAction('[Auth] Logout');

// Load User from Storage (on app init)
export const loadUserFromStorage = createAction('[Auth] Load User from Storage');

export const loadUserFromStorageSuccess = createAction(
  '[Auth] Load User from Storage Success',
  props<{ accessToken: string; refreshToken: string; user: User }>()
);

export const loadUserFromStorageFailure = createAction(
  '[Auth] Load User from Storage Failure'
);

