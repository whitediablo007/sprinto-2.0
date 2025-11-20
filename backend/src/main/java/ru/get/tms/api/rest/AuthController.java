package ru.get.tms.api.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.get.tms.dto.auth.AuthResponse;
import ru.get.tms.dto.auth.LoginRequest;
import ru.get.tms.dto.auth.PasswordResetConfirm;
import ru.get.tms.dto.auth.PasswordResetRequest;
import ru.get.tms.dto.auth.RegisterRequest;
import ru.get.tms.service.AuthService;
import ru.get.tms.service.PasswordResetService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final PasswordResetService passwordResetService;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/refresh")
  public Mono<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
    return authService.refreshToken(request.getRefreshToken());
  }

  /**
   * Request password reset (FR-007.1).
   *
   * <p>Sends password reset email if user exists. Always returns 200 to prevent user enumeration.
   *
   * @param request password reset request with email
   * @return Mono&lt;Void&gt; always successful
   */
  @PostMapping("/password-reset/request")
  public Mono<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
    return passwordResetService.requestPasswordReset(request.getEmail());
  }

  /**
   * Confirm password reset with token (FR-007.1).
   *
   * @param request password reset confirmation with token and new password
   * @return Mono&lt;Void&gt; successful if token is valid and password is updated
   */
  @PostMapping("/password-reset/confirm")
  public Mono<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
    return passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
  }

  // Inner class for refresh token request
  public static class RefreshTokenRequest {
    private String refreshToken;

    public String getRefreshToken() {
      return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
      this.refreshToken = refreshToken;
    }
  }
}
