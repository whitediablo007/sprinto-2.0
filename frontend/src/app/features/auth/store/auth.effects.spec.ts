import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, of, throwError, EMPTY } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { AuthEffects } from './auth.effects';
import { AuthService, AuthResponse } from '../../../core/auth/auth.service';
import * as AuthActions from './auth.actions';

/**
 * Unit tests for Auth Effects.
 * 
 * Tests side effects for authentication actions including:
 * - API calls with mocked AuthService
 * - localStorage operations
 * - Router navigation
 * 
 * Uses provideMockActions and RxJS TestScheduler for effect testing.
 * 
 * @see AuthEffects
 */
describe('AuthEffects', () => {
  let actions$: Observable<any>;
  let effects: AuthEffects;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;
  let testScheduler: TestScheduler;
  let localStorageSpy: jasmine.Spy;

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

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', [
      'login',
      'register',
      'refreshToken'
    ]);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        AuthEffects,
        provideMockActions(() => actions$),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    effects = TestBed.inject(AuthEffects);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    // Setup test scheduler for marble testing
    testScheduler = new TestScheduler((actual, expected) => {
      expect(actual).toEqual(expected);
    });

    // Mock localStorage
    let store: { [key: string]: string } = {};
    localStorageSpy = spyOn(localStorage, 'getItem').and.callFake((key: string) => store[key] || null);
    spyOn(localStorage, 'setItem').and.callFake((key: string, value: string) => {
      store[key] = value;
    });
    spyOn(localStorage, 'removeItem').and.callFake((key: string) => {
      delete store[key];
    });
    spyOn(localStorage, 'clear').and.callFake(() => {
      store = {};
    });
  });

  // ========== LOGIN EFFECTS ==========

  describe('login$', () => {
    it('should return loginSuccess action with response on successful login', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        const credentials = { email: 'test@example.com', password: 'password123' };
        const action = AuthActions.login({ credentials });
        const outcome = AuthActions.loginSuccess({ response: mockAuthResponse });

        actions$ = hot('-a', { a: action });
        authService.login.and.returnValue(of(mockAuthResponse));

        // Act & Assert
        expectObservable(effects.login$).toBe('-c', { c: outcome });
      });
    });

    it('should return loginFailure action on login error', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        const credentials = { email: 'test@example.com', password: 'wrong' };
        const action = AuthActions.login({ credentials });
        const error = { error: { message: 'Invalid credentials' } };
        const outcome = AuthActions.loginFailure({ error: 'Invalid credentials' });

        actions$ = hot('-a', { a: action });
        authService.login.and.returnValue(throwError(() => error) as Observable<AuthResponse>);

        // Act & Assert
        expectObservable(effects.login$).toBe('-c', { c: outcome });
      });
    });
  });

  // ========== REGISTER EFFECTS ==========

  describe('register$', () => {
    it('should return registerSuccess action with response on successful registration', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        const userData = { 
          email: 'test@example.com', 
          password: 'password123', 
          name: 'Test User' 
        };
        const action = AuthActions.register({ userData });
        const outcome = AuthActions.registerSuccess({ response: mockAuthResponse });

        actions$ = hot('-a', { a: action });
        authService.register.and.returnValue(of(mockAuthResponse));

        // Act & Assert
        expectObservable(effects.register$).toBe('-c', { c: outcome });
      });
    });

    it('should return registerFailure action on registration error', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        const userData = { 
          email: 'test@example.com', 
          password: 'password123', 
          name: 'Test User' 
        };
        const action = AuthActions.register({ userData });
        const error = { error: { message: 'Email already exists' } };
        const outcome = AuthActions.registerFailure({ error: 'Email already exists' });

        actions$ = hot('-a', { a: action });
        authService.register.and.returnValue(throwError(() => error) as Observable<AuthResponse>);

        // Act & Assert
        expectObservable(effects.register$).toBe('-c', { c: outcome });
      });
    });
  });

  // ========== REFRESH TOKEN EFFECTS ==========

  describe('refreshToken$', () => {
    it('should return refreshTokenSuccess action on successful token refresh', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        const refreshToken = 'mock-refresh-token';
        const action = AuthActions.refreshToken({ refreshToken });
        const outcome = AuthActions.refreshTokenSuccess({ response: mockAuthResponse });

        actions$ = hot('-a', { a: action });
        authService.refreshToken.and.returnValue(of(mockAuthResponse));

        // Act & Assert
        expectObservable(effects.refreshToken$).toBe('-c', { c: outcome });
      });
    });

    it('should return refreshTokenFailure action on token refresh error', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        const refreshToken = 'invalid-token';
        const action = AuthActions.refreshToken({ refreshToken });
        const error = { error: { message: 'Token refresh failed' } };
        const outcome = AuthActions.refreshTokenFailure({ error: 'Token refresh failed' });

        actions$ = hot('-a', { a: action });
        authService.refreshToken.and.returnValue(throwError(() => error) as Observable<AuthResponse>);

        // Act & Assert
        expectObservable(effects.refreshToken$).toBe('-c', { c: outcome });
      });
    });
  });

  // ========== STORE AUTH DATA EFFECT ==========

  describe('storeAuthData$', () => {
    it('should store tokens and user in localStorage on loginSuccess', (done) => {
      // Arrange
      actions$ = of(AuthActions.loginSuccess({ response: mockAuthResponse }));

      // Act
      effects.storeAuthData$.subscribe(() => {
        // Assert
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'access_token',
          mockAuthResponse.accessToken
        );
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'refresh_token',
          mockAuthResponse.refreshToken
        );
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'user',
          JSON.stringify(mockAuthResponse.user)
        );
        done();
      });
    });

    it('should store tokens and user in localStorage on registerSuccess', (done) => {
      // Arrange
      actions$ = of(AuthActions.registerSuccess({ response: mockAuthResponse }));

      // Act
      effects.storeAuthData$.subscribe(() => {
        // Assert
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'access_token',
          mockAuthResponse.accessToken
        );
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'refresh_token',
          mockAuthResponse.refreshToken
        );
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'user',
          JSON.stringify(mockAuthResponse.user)
        );
        done();
      });
    });

    it('should store tokens and user in localStorage on refreshTokenSuccess', (done) => {
      // Arrange
      actions$ = of(AuthActions.refreshTokenSuccess({ response: mockAuthResponse }));

      // Act
      effects.storeAuthData$.subscribe(() => {
        // Assert
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'access_token',
          mockAuthResponse.accessToken
        );
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'refresh_token',
          mockAuthResponse.refreshToken
        );
        expect(localStorage.setItem).toHaveBeenCalledWith(
          'user',
          JSON.stringify(mockAuthResponse.user)
        );
        done();
      });
    });
  });

  // ========== NAVIGATION EFFECTS ==========

  describe('navigateToDashboard$', () => {
    it('should navigate to dashboard on loginSuccess', (done) => {
      // Arrange
      actions$ = of(AuthActions.loginSuccess({ response: mockAuthResponse }));

      // Act
      effects.navigateToDashboard$.subscribe(() => {
        // Assert
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
        done();
      });
    });

    it('should navigate to dashboard on registerSuccess', (done) => {
      // Arrange
      actions$ = of(AuthActions.registerSuccess({ response: mockAuthResponse }));

      // Act
      effects.navigateToDashboard$.subscribe(() => {
        // Assert
        expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
        done();
      });
    });
  });

  // ========== LOGOUT EFFECT ==========

  describe('logout$', () => {
    it('should clear localStorage and navigate to login on logout', (done) => {
      // Arrange
      actions$ = of(AuthActions.logout());

      // Act
      effects.logout$.subscribe(() => {
        // Assert
        expect(localStorage.removeItem).toHaveBeenCalledWith('access_token');
        expect(localStorage.removeItem).toHaveBeenCalledWith('refresh_token');
        expect(localStorage.removeItem).toHaveBeenCalledWith('user');
        expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
        done();
      });
    });
  });

  // ========== LOAD USER FROM STORAGE EFFECT ==========

  describe('loadUserFromStorage$', () => {
    it('should return loadUserFromStorageSuccess when tokens exist in localStorage', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        localStorage.setItem('access_token', mockAuthResponse.accessToken);
        localStorage.setItem('refresh_token', mockAuthResponse.refreshToken);
        localStorage.setItem('user', JSON.stringify(mockAuthResponse.user));

        const action = AuthActions.loadUserFromStorage();
        const outcome = AuthActions.loadUserFromStorageSuccess({
          accessToken: mockAuthResponse.accessToken,
          refreshToken: mockAuthResponse.refreshToken,
          user: mockAuthResponse.user
        });

        actions$ = hot('-a', { a: action });

        // Act & Assert
        expectObservable(effects.loadUserFromStorage$).toBe('-c', { c: outcome });
      });
    });

    it('should return loadUserFromStorageFailure when no tokens in localStorage', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        const action = AuthActions.loadUserFromStorage();
        const outcome = AuthActions.loadUserFromStorageFailure();

        actions$ = hot('-a', { a: action });

        // Act & Assert
        expectObservable(effects.loadUserFromStorage$).toBe('-c', { c: outcome });
      });
    });

    it('should return loadUserFromStorageFailure when user data is incomplete', () => {
      testScheduler.run(({ hot, expectObservable }) => {
        // Arrange
        localStorage.setItem('access_token', mockAuthResponse.accessToken);
        // Missing refreshToken and user

        const action = AuthActions.loadUserFromStorage();
        const outcome = AuthActions.loadUserFromStorageFailure();

        actions$ = hot('-a', { a: action });

        // Act & Assert
        expectObservable(effects.loadUserFromStorage$).toBe('-c', { c: outcome });
      });
    });
  });

  // ========== ERROR HANDLING ==========

  describe('Error Handling', () => {
    it('should handle error without message property', () => {
      testScheduler.run(({ hot, cold, expectObservable }) => {
        // Arrange
        const credentials = { email: 'test@example.com', password: 'wrong' };
        const action = AuthActions.login({ credentials });
        const error = { error: {} }; // No message property
        const outcome = AuthActions.loginFailure({ error: 'Login failed' });

        actions$ = hot('-a', { a: action });
        const response = throwError(() => error) as Observable<AuthResponse>;
        authService.login.and.returnValue(response);

        // Act & Assert
        expectObservable(effects.login$).toBe('--c', { c: outcome });
      });
    });

    it('should handle null error response', () => {
      testScheduler.run(({ hot, cold, expectObservable }) => {
        // Arrange
        const userData = { 
          email: 'test@example.com', 
          password: 'password123', 
          name: 'Test User' 
        };
        const action = AuthActions.register({ userData });
        const error = null;
        const outcome = AuthActions.registerFailure({ error: 'Registration failed' });

        actions$ = hot('-a', { a: action });
        const response = throwError(() => error) as Observable<AuthResponse>;
        authService.register.and.returnValue(response);

        // Act & Assert
        expectObservable(effects.register$).toBe('--c', { c: outcome });
      });
    });
  });
});



