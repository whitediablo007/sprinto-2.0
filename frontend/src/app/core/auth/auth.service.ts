import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Authentication request/response interfaces.
 */
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface User {
  id: string;
  email: string;
  name: string;
  role: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface PasswordResetRequest {
  email: string;
}

export interface PasswordResetConfirm {
  token: string;
  newPassword: string;
}

/**
 * Authentication Service.
 * 
 * Provides methods for user authentication including:
 * - Login/Register
 * - Token refresh
 * - Password reset
 * - Logout
 * 
 * @see FR-007 for authentication requirements
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;

  constructor(private http: HttpClient) {}

  /**
   * Login with email and password.
   * 
   * @param credentials - User credentials
   * @returns Observable with authentication response
   */
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials);
  }

  /**
   * Register a new user account.
   * 
   * @param userData - Registration data
   * @returns Observable with authentication response
   */
  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, userData);
  }

  /**
   * Refresh access token using refresh token.
   * 
   * @param refreshToken - Current refresh token
   * @returns Observable with new authentication response
   */
  refreshToken(refreshToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/refresh`, { refreshToken });
  }

  /**
   * Request password reset (sends email with reset link).
   * 
   * @param email - User email address
   * @returns Observable indicating success
   */
  requestPasswordReset(email: string): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/password-reset/request`, { email });
  }

  /**
   * Confirm password reset with token and new password.
   * 
   * @param data - Reset token and new password
   * @returns Observable indicating success
   */
  confirmPasswordReset(data: PasswordResetConfirm): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/password-reset/confirm`, data);
  }

  /**
   * Logout (client-side only - clears tokens from store).
   * Server-side logout is not implemented as JWT tokens are stateless.
   */
  logout(): void {
    // Logout is handled by clearing tokens in NgRx store
    // No server-side endpoint needed for stateless JWT
  }
}

