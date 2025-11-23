import { createFeatureSelector, createSelector } from '@ngrx/store';
import { AuthState } from './auth.reducer';

/**
 * Auth Selectors.
 * 
 * Provides selectors for accessing auth state from NgRx store.
 */

/**
 * Select the auth state slice.
 */
export const selectAuthState = createFeatureSelector<AuthState>('auth');

/**
 * Select access token.
 */
export const selectAccessToken = createSelector(
  selectAuthState,
  (state: AuthState) => state.accessToken
);

/**
 * Select refresh token.
 */
export const selectRefreshToken = createSelector(
  selectAuthState,
  (state: AuthState) => state.refreshToken
);

/**
 * Select current user.
 */
export const selectCurrentUser = createSelector(
  selectAuthState,
  (state: AuthState) => state.user
);

/**
 * Alias for selectCurrentUser.
 */
export const selectAuthUser = selectCurrentUser;

/**
 * Select authentication status.
 */
export const selectIsAuthenticated = createSelector(
  selectAuthState,
  (state: AuthState) => state.isAuthenticated
);

/**
 * Select loading status.
 */
export const selectAuthLoading = createSelector(
  selectAuthState,
  (state: AuthState) => state.loading
);

/**
 * Select error message.
 */
export const selectAuthError = createSelector(
  selectAuthState,
  (state: AuthState) => state.error
);

/**
 * Select user ID.
 */
export const selectUserId = createSelector(
  selectCurrentUser,
  (user) => user?.id
);

/**
 * Select user email.
 */
export const selectUserEmail = createSelector(
  selectCurrentUser,
  (user) => user?.email
);

/**
 * Select user name.
 */
export const selectUserName = createSelector(
  selectCurrentUser,
  (user) => user?.name
);

/**
 * Select user role.
 */
export const selectUserRole = createSelector(
  selectCurrentUser,
  (user) => user?.role
);

