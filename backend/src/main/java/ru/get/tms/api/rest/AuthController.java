package ru.get.tms.api.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.get.tms.dto.auth.AuthResponse;
import ru.get.tms.dto.auth.LoginRequest;
import ru.get.tms.dto.auth.RegisterRequest;
import ru.get.tms.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

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
