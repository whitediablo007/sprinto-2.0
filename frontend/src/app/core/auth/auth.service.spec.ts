import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AuthService, LoginRequest, RegisterRequest, AuthResponse, PasswordResetConfirm } from './auth.service';
import { environment } from '../../../environments/environment';

/**
 * Unit tests for AuthService.
 * 
 * Tests HTTP calls for authentication with mocked HttpClient.
 * Uses Angular's HttpClientTestingModule for HTTP testing.
 * 
 * @see AuthService
 */
describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

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
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AuthService]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    // Verify that no unmatched requests are outstanding
    httpMock.verify();
  });

  // ========== SERVICE INITIALIZATION ==========

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ========== LOGIN TESTS ==========

  describe('login', () => {
    it('should make POST request to /api/auth/login with credentials', () => {
      // Arrange
      const credentials: LoginRequest = {
        email: 'test@example.com',
        password: 'password123'
      };

      // Act
      service.login(credentials).subscribe(response => {
        // Assert
        expect(response).toEqual(mockAuthResponse);
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(credentials);

      // Respond with mock data
      req.flush(mockAuthResponse);
    });

    it('should handle login error', () => {
      // Arrange
      const credentials: LoginRequest = {
        email: 'test@example.com',
        password: 'wrong-password'
      };
      const errorMessage = 'Invalid credentials';

      // Act
      service.login(credentials).subscribe({
        next: () => fail('Should have failed with error'),
        error: (error) => {
          // Assert
          expect(error.status).toBe(401);
          expect(error.error.message).toBe(errorMessage);
        }
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
      expect(req.request.method).toBe('POST');

      // Respond with error
      req.flush({ message: errorMessage }, { status: 401, statusText: 'Unauthorized' });
    });
  });

  // ========== REGISTER TESTS ==========

  describe('register', () => {
    it('should make POST request to /api/auth/register with user data', () => {
      // Arrange
      const userData: RegisterRequest = {
        email: 'newuser@example.com',
        password: 'password123',
        name: 'New User'
      };

      // Act
      service.register(userData).subscribe(response => {
        // Assert
        expect(response).toEqual(mockAuthResponse);
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(userData);

      // Respond with mock data
      req.flush(mockAuthResponse);
    });

    it('should handle registration error when email already exists', () => {
      // Arrange
      const userData: RegisterRequest = {
        email: 'existing@example.com',
        password: 'password123',
        name: 'Existing User'
      };
      const errorMessage = 'Email already exists';

      // Act
      service.register(userData).subscribe({
        next: () => fail('Should have failed with error'),
        error: (error) => {
          // Assert
          expect(error.status).toBe(409);
          expect(error.error.message).toBe(errorMessage);
        }
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/register`);
      expect(req.request.method).toBe('POST');

      // Respond with error
      req.flush({ message: errorMessage }, { status: 409, statusText: 'Conflict' });
    });
  });

  // ========== REFRESH TOKEN TESTS ==========

  describe('refreshToken', () => {
    it('should make POST request to /api/auth/refresh with refresh token', () => {
      // Arrange
      const refreshToken = 'mock-refresh-token';

      // Act
      service.refreshToken(refreshToken).subscribe(response => {
        // Assert
        expect(response).toEqual(mockAuthResponse);
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken });

      // Respond with mock data
      req.flush(mockAuthResponse);
    });

    it('should handle refresh token error when token is invalid', () => {
      // Arrange
      const refreshToken = 'invalid-token';
      const errorMessage = 'Invalid refresh token';

      // Act
      service.refreshToken(refreshToken).subscribe({
        next: () => fail('Should have failed with error'),
        error: (error) => {
          // Assert
          expect(error.status).toBe(401);
          expect(error.error.message).toBe(errorMessage);
        }
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
      expect(req.request.method).toBe('POST');

      // Respond with error
      req.flush({ message: errorMessage }, { status: 401, statusText: 'Unauthorized' });
    });
  });

  // ========== PASSWORD RESET TESTS ==========

  describe('requestPasswordReset', () => {
    it('should make POST request to /api/auth/password-reset/request with email', () => {
      // Arrange
      const email = 'test@example.com';

      // Act
      service.requestPasswordReset(email).subscribe(response => {
        // Assert
        expect(response).toBeUndefined(); // Void response
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/password-reset/request`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ email });

      // Respond with success
      req.flush(null);
    });

    it('should handle password reset request error', () => {
      // Arrange
      const email = 'invalid@example.com';
      const errorMessage = 'User not found';

      // Act
      service.requestPasswordReset(email).subscribe({
        next: () => fail('Should have failed with error'),
        error: (error) => {
          // Assert
          expect(error.status).toBe(404);
          expect(error.error.message).toBe(errorMessage);
        }
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/password-reset/request`);
      expect(req.request.method).toBe('POST');

      // Respond with error
      req.flush({ message: errorMessage }, { status: 404, statusText: 'Not Found' });
    });
  });

  describe('confirmPasswordReset', () => {
    it('should make POST request to /api/auth/password-reset/confirm with token and password', () => {
      // Arrange
      const resetData: PasswordResetConfirm = {
        token: 'reset-token-123',
        newPassword: 'newPassword123'
      };

      // Act
      service.confirmPasswordReset(resetData).subscribe(response => {
        // Assert
        expect(response).toBeUndefined(); // Void response
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/password-reset/confirm`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(resetData);

      // Respond with success
      req.flush(null);
    });

    it('should handle password reset confirmation error when token is invalid', () => {
      // Arrange
      const resetData: PasswordResetConfirm = {
        token: 'invalid-token',
        newPassword: 'newPassword123'
      };
      const errorMessage = 'Invalid or expired token';

      // Act
      service.confirmPasswordReset(resetData).subscribe({
        next: () => fail('Should have failed with error'),
        error: (error) => {
          // Assert
          expect(error.status).toBe(400);
          expect(error.error.message).toBe(errorMessage);
        }
      });

      // Assert HTTP request
      const req = httpMock.expectOne(`${environment.apiUrl}/auth/password-reset/confirm`);
      expect(req.request.method).toBe('POST');

      // Respond with error
      req.flush({ message: errorMessage }, { status: 400, statusText: 'Bad Request' });
    });
  });

  // ========== LOGOUT TESTS ==========

  describe('logout', () => {
    it('should execute logout method (no-op)', () => {
      // Act
      service.logout();

      // Assert - No HTTP request expected
      httpMock.expectNone(() => true);
    });
  });

  // ========== API URL CONFIGURATION TESTS ==========

  it('should use correct API URL from environment', () => {
    // Arrange
    const credentials: LoginRequest = {
      email: 'test@example.com',
      password: 'password123'
    };

    // Act
    service.login(credentials).subscribe();

    // Assert
    const req = httpMock.expectOne((request) => {
      return request.url.includes(environment.apiUrl);
    });
    expect(req.request.url).toBe(`${environment.apiUrl}/auth/login`);

    req.flush(mockAuthResponse);
  });
});



