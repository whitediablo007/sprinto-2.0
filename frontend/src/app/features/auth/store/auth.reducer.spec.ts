import { authReducer, initialAuthState, AuthState } from './auth.reducer';
import * as AuthActions from './auth.actions';
import { AuthResponse, User } from '../../../core/auth/auth.service';

/**
 * Unit tests for Auth Reducer.
 * 
 * Tests state transitions for authentication actions.
 * Ensures reducer is pure and produces correct state changes.
 * 
 * @see authReducer
 */
describe('Auth Reducer', () => {
  const mockAuthResponse: AuthResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    user: {
      id: '123e4567-e89b-12d3-a456-426614174000',
      email: 'test@example.com',
      name: 'Test User',
      role: 'USER'
    }
  };

  const mockUser: User = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    email: 'test@example.com',
    name: 'Test User',
    role: 'USER'
  };

  // ========== INITIAL STATE ==========

  describe('initialAuthState', () => {
    it('should have correct initial state', () => {
      expect(initialAuthState).toEqual({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
        loading: false,
        error: null
      });
    });
  });

  // ========== UNKNOWN ACTION ==========

  describe('unknown action', () => {
    it('should return the previous state', () => {
      const action = {} as any;
      const result = authReducer(initialAuthState, action);

      expect(result).toBe(initialAuthState);
    });
  });

  // ========== LOGIN ACTIONS ==========

  describe('Login Actions', () => {
    it('should set loading to true on login action', () => {
      const credentials = { email: 'test@example.com', password: 'password123' };
      const action = AuthActions.login({ credentials });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        ...initialAuthState,
        loading: true,
        error: null
      });
    });

    it('should set authentication data on loginSuccess', () => {
      const action = AuthActions.loginSuccess({ response: mockAuthResponse });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        accessToken: mockAuthResponse.accessToken,
        refreshToken: mockAuthResponse.refreshToken,
        user: mockAuthResponse.user,
        isAuthenticated: true,
        loading: false,
        error: null
      });
    });

    it('should preserve existing state when login succeeds', () => {
      const previousState: AuthState = {
        ...initialAuthState,
        loading: true
      };
      const action = AuthActions.loginSuccess({ response: mockAuthResponse });
      const state = authReducer(previousState, action);

      expect(state.loading).toBe(false);
      expect(state.isAuthenticated).toBe(true);
      expect(state.accessToken).toBe(mockAuthResponse.accessToken);
    });

    it('should set error on loginFailure', () => {
      const error = 'Invalid credentials';
      const action = AuthActions.loginFailure({ error });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        ...initialAuthState,
        loading: false,
        error
      });
    });
  });

  // ========== REGISTER ACTIONS ==========

  describe('Register Actions', () => {
    it('should set loading to true on register action', () => {
      const userData = { 
        email: 'test@example.com', 
        password: 'password123', 
        name: 'Test User' 
      };
      const action = AuthActions.register({ userData });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        ...initialAuthState,
        loading: true,
        error: null
      });
    });

    it('should set authentication data on registerSuccess', () => {
      const action = AuthActions.registerSuccess({ response: mockAuthResponse });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        accessToken: mockAuthResponse.accessToken,
        refreshToken: mockAuthResponse.refreshToken,
        user: mockAuthResponse.user,
        isAuthenticated: true,
        loading: false,
        error: null
      });
    });

    it('should set error on registerFailure', () => {
      const error = 'Email already exists';
      const action = AuthActions.registerFailure({ error });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        ...initialAuthState,
        loading: false,
        error
      });
    });
  });

  // ========== REFRESH TOKEN ACTIONS ==========

  describe('Refresh Token Actions', () => {
    it('should set loading to true on refreshToken action', () => {
      const refreshToken = 'mock-refresh-token';
      const action = AuthActions.refreshToken({ refreshToken });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        ...initialAuthState,
        loading: true
      });
    });

    it('should update tokens on refreshTokenSuccess', () => {
      const previousState: AuthState = {
        ...initialAuthState,
        accessToken: 'old-access-token',
        refreshToken: 'old-refresh-token',
        user: mockUser,
        isAuthenticated: true,
        loading: true
      };

      const newTokenResponse: AuthResponse = {
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        user: mockUser
      };

      const action = AuthActions.refreshTokenSuccess({ response: newTokenResponse });
      const state = authReducer(previousState, action);

      expect(state).toEqual({
        accessToken: 'new-access-token',
        refreshToken: 'new-refresh-token',
        user: mockUser,
        isAuthenticated: true,
        loading: false,
        error: null
      });
    });

    it('should clear authentication on refreshTokenFailure', () => {
      const previousState: AuthState = {
        ...initialAuthState,
        accessToken: 'expired-access-token',
        refreshToken: 'invalid-refresh-token',
        user: mockUser,
        isAuthenticated: true,
        loading: true
      };

      const error = 'Token refresh failed';
      const action = AuthActions.refreshTokenFailure({ error });
      const state = authReducer(previousState, action);

      expect(state).toEqual({
        accessToken: null,
        refreshToken: null,
        user: null,
        isAuthenticated: false,
        loading: false,
        error
      });
    });
  });

  // ========== LOGOUT ACTION ==========

  describe('Logout Action', () => {
    it('should reset to initial state on logout', () => {
      const authenticatedState: AuthState = {
        accessToken: 'token',
        refreshToken: 'refresh',
        user: mockUser,
        isAuthenticated: true,
        loading: false,
        error: null
      };

      const action = AuthActions.logout();
      const state = authReducer(authenticatedState, action);

      expect(state).toEqual(initialAuthState);
    });

    it('should clear errors on logout', () => {
      const stateWithError: AuthState = {
        ...initialAuthState,
        error: 'Some error'
      };

      const action = AuthActions.logout();
      const state = authReducer(stateWithError, action);

      expect(state.error).toBeNull();
    });
  });

  // ========== LOAD USER FROM STORAGE ACTIONS ==========

  describe('Load User from Storage Actions', () => {
    it('should set loading to true on loadUserFromStorage', () => {
      const action = AuthActions.loadUserFromStorage();
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        ...initialAuthState,
        loading: true
      });
    });

    it('should set authentication data on loadUserFromStorageSuccess', () => {
      const accessToken = 'stored-access-token';
      const refreshToken = 'stored-refresh-token';
      const user = mockUser;

      const action = AuthActions.loadUserFromStorageSuccess({ 
        accessToken, 
        refreshToken, 
        user 
      });
      const state = authReducer(initialAuthState, action);

      expect(state).toEqual({
        accessToken,
        refreshToken,
        user,
        isAuthenticated: true,
        loading: false,
        error: null
      });
    });

    it('should set loading to false on loadUserFromStorageFailure', () => {
      const previousState: AuthState = {
        ...initialAuthState,
        loading: true
      };

      const action = AuthActions.loadUserFromStorageFailure();
      const state = authReducer(previousState, action);

      expect(state).toEqual({
        ...initialAuthState,
        loading: false
      });
    });
  });

  // ========== STATE IMMUTABILITY ==========

  describe('State Immutability', () => {
    it('should not mutate the previous state', () => {
      const previousState: AuthState = {
        ...initialAuthState
      };
      const credentials = { email: 'test@example.com', password: 'password123' };
      const action = AuthActions.login({ credentials });

      const state = authReducer(previousState, action);

      expect(state).not.toBe(previousState);
      expect(previousState.loading).toBe(false); // Previous state unchanged
      expect(state.loading).toBe(true); // New state has changes
    });
  });

  // ========== SEQUENTIAL ACTIONS ==========

  describe('Sequential Actions', () => {
    it('should handle sequence of login -> success correctly', () => {
      const credentials = { email: 'test@example.com', password: 'password123' };
      
      // Login action
      const loginAction = AuthActions.login({ credentials });
      let state = authReducer(initialAuthState, loginAction);

      expect(state.loading).toBe(true);
      expect(state.isAuthenticated).toBe(false);

      // Login success action
      const successAction = AuthActions.loginSuccess({ response: mockAuthResponse });
      state = authReducer(state, successAction);

      expect(state.loading).toBe(false);
      expect(state.isAuthenticated).toBe(true);
      expect(state.accessToken).toBe(mockAuthResponse.accessToken);
      expect(state.error).toBeNull();
    });

    it('should handle sequence of login -> failure correctly', () => {
      const credentials = { email: 'test@example.com', password: 'wrong' };
      
      // Login action
      const loginAction = AuthActions.login({ credentials });
      let state = authReducer(initialAuthState, loginAction);

      expect(state.loading).toBe(true);

      // Login failure action
      const error = 'Invalid credentials';
      const failureAction = AuthActions.loginFailure({ error });
      state = authReducer(state, failureAction);

      expect(state.loading).toBe(false);
      expect(state.isAuthenticated).toBe(false);
      expect(state.error).toBe(error);
      expect(state.accessToken).toBeNull();
    });
  });
});



