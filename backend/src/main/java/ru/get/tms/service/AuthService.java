package ru.get.tms.service;

import io.r2dbc.postgresql.codec.Json;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.user.User;
import ru.get.tms.dto.auth.AuthResponse;
import ru.get.tms.dto.auth.LoginRequest;
import ru.get.tms.dto.auth.RegisterRequest;
import ru.get.tms.exception.ConflictException;
import ru.get.tms.exception.UnauthorizedException;
import ru.get.tms.repository.UserRepository;
import ru.get.tms.security.JwtUtil;
import ru.get.tms.util.ReactiveErrorHandler;

/**
 * Service for user authentication and authorization (NFR-027).
 *
 * <p>Implements reactive error handling pattern:
 *
 * <ul>
 *   <li>Uses domain exceptions (ConflictException, UnauthorizedException)
 *   <li>Applies ReactiveErrorHandler for consistent error mapping
 *   <li>No blocking operations, pure reactive pipeline
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  /**
   * Register new user.
   *
   * @param request registration request
   * @return Mono with authentication response
   * @throws ConflictException if email already exists
   */
  public Mono<AuthResponse> register(RegisterRequest request) {
    log.debug("Registering new user: {}", request.getEmail());

    return userRepository
        .existsByEmail(request.getEmail())
        .flatMap(
            exists -> {
              if (exists) {
                log.warn("Registration failed: email already exists - {}", request.getEmail());
                return Mono.error(
                    new ConflictException(
                        "User with email " + request.getEmail() + " already exists"));
              }

              User user =
                  User.builder()
                      .email(request.getEmail())
                      .passwordHash(passwordEncoder.encode(request.getPassword()))
                      .name(request.getName())
                      .isAdmin(false)
                      .notificationPreferences(Json.of("{}".getBytes(StandardCharsets.UTF_8)))
                      .timezone("Europe/Moscow")
                      .createdAt(LocalDateTime.now())
                      .updatedAt(LocalDateTime.now())
                      .build();

              return userRepository
                  .save(user)
                  .doOnSuccess(u -> log.info("User registered successfully: {}", u.getEmail()))
                  .map(
                      savedUser -> {
                        String accessToken =
                            jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getEmail());
                        String refreshToken =
                            jwtUtil.generateRefreshToken(savedUser.getId(), savedUser.getEmail());

                        return AuthResponse.builder()
                            .userId(savedUser.getId())
                            .email(savedUser.getEmail())
                            .name(savedUser.getName())
                            .accessToken(accessToken)
                            .refreshToken(refreshToken)
                            .build();
                      });
            })
        .onErrorMap(ReactiveErrorHandler.mapDatabaseError())
        .onErrorResume(ReactiveErrorHandler::handleError);
  }

  /**
   * Authenticate user.
   *
   * @param request login request
   * @return Mono with authentication response
   * @throws UnauthorizedException if credentials are invalid
   */
  public Mono<AuthResponse> login(LoginRequest request) {
    log.debug("Login attempt for email: {}", request.getEmail());

    return userRepository
        .findByEmail(request.getEmail())
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn("Login failed: user not found - {}", request.getEmail());
                  return Mono.error(new UnauthorizedException("Invalid email or password"));
                }))
        .flatMap(
            user -> {
              if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                log.warn("Login failed: invalid password - {}", request.getEmail());
                return Mono.error(new UnauthorizedException("Invalid email or password"));
              }

              log.info("User logged in successfully: {}", user.getEmail());

              String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
              String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

              return Mono.just(
                  AuthResponse.builder()
                      .userId(user.getId())
                      .email(user.getEmail())
                      .name(user.getName())
                      .accessToken(accessToken)
                      .refreshToken(refreshToken)
                      .build());
            })
        .onErrorResume(ReactiveErrorHandler::handleError);
  }

  /**
   * Refresh access token using refresh token.
   *
   * @param refreshToken refresh token
   * @return Mono with new authentication response
   * @throws UnauthorizedException if token is invalid or expired
   */
  public Mono<AuthResponse> refreshToken(String refreshToken) {
    log.debug("Refreshing token");

    return Mono.fromCallable(
            () -> {
              if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new UnauthorizedException("Invalid refresh token");
              }

              if (jwtUtil.isTokenExpired(refreshToken)) {
                throw new UnauthorizedException("Refresh token expired");
              }

              return jwtUtil.getUserIdFromToken(refreshToken);
            })
        .flatMap(
            userId ->
                userRepository
                    .findById(userId)
                    .switchIfEmpty(ReactiveErrorHandler.handleNotFound("User", userId))
                    .doOnSuccess(u -> log.debug("Token refreshed for user: {}", u.getEmail()))
                    .map(
                        user -> {
                          String newAccessToken =
                              jwtUtil.generateAccessToken(user.getId(), user.getEmail());
                          String newRefreshToken =
                              jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

                          return AuthResponse.builder()
                              .userId(user.getId())
                              .email(user.getEmail())
                              .name(user.getName())
                              .accessToken(newAccessToken)
                              .refreshToken(newRefreshToken)
                              .build();
                        }))
        .onErrorResume(ReactiveErrorHandler::handleError);
  }
}
