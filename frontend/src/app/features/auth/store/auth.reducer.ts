import { createReducer, on } from '@ngrx/store';
import { User } from '../../../core/auth/auth.service';
import * as AuthActions from './auth.actions';

/**
 * Auth State interface.
 */
export interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  loading: boolean;
  error: string | null;
}

/**
 * Initial auth state.
 */
export const initialAuthState: AuthState = {
  accessToken: null,
  refreshToken: null,
  user: null,
  isAuthenticated: false,
  loading: false,
  error: null
};

/**
 * Auth Reducer.
 * 
 * Handles state changes for authentication actions.
 */
export const authReducer = createReducer(
  initialAuthState,

  // Login
  on(AuthActions.login, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(AuthActions.loginSuccess, (state, { response }) => ({
    ...state,
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    user: {
      id: response.userId,
      email: response.email,
      name: response.name
    },
    isAuthenticated: true,
    loading: false,
    error: null
  })),

  on(AuthActions.loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Register
  on(AuthActions.register, (state) => ({
    ...state,
    loading: true,
    error: null
  })),

  on(AuthActions.registerSuccess, (state, { response }) => ({
    ...state,
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    user: {
      id: response.userId,
      email: response.email,
      name: response.name
    },
    isAuthenticated: true,
    loading: false,
    error: null
  })),

  on(AuthActions.registerFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error
  })),

  // Refresh Token
  on(AuthActions.refreshToken, (state) => ({
    ...state,
    loading: true
  })),

  on(AuthActions.refreshTokenSuccess, (state, { response }) => ({
    ...state,
    accessToken: response.accessToken,
    refreshToken: response.refreshToken,
    user: {
      id: response.userId,
      email: response.email,
      name: response.name
    },
    isAuthenticated: true,
    loading: false,
    error: null
  })),

  on(AuthActions.refreshTokenFailure, (state, { error }) => ({
    ...state,
    accessToken: null,
    refreshToken: null,
    user: null,
    isAuthenticated: false,
    loading: false,
    error
  })),

  // Logout
  on(AuthActions.logout, () => initialAuthState),

  // Load from Storage
  on(AuthActions.loadUserFromStorage, (state) => ({
    ...state,
    loading: true
  })),

  on(AuthActions.loadUserFromStorageSuccess, (state, { accessToken, refreshToken, user }) => ({
    ...state,
    accessToken,
    refreshToken,
    user,
    isAuthenticated: true,
    loading: false
  })),

  on(AuthActions.loadUserFromStorageFailure, (state) => ({
    ...state,
    loading: false
  }))
);

