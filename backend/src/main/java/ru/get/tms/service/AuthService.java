package ru.get.tms.service;

import io.r2dbc.postgresql.codec.Json;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.user.User;
import ru.get.tms.dto.auth.AuthResponse;
import ru.get.tms.dto.auth.LoginRequest;
import ru.get.tms.dto.auth.RegisterRequest;
import ru.get.tms.repository.UserRepository;
import ru.get.tms.security.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public Mono<AuthResponse> register(RegisterRequest request) {
    return userRepository
        .existsByEmail(request.getEmail())
        .flatMap(
            exists -> {
              if (exists) {
                return Mono.error(
                    new RuntimeException(
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
            });
  }

  public Mono<AuthResponse> login(LoginRequest request) {
    return userRepository
        .findByEmail(request.getEmail())
        .switchIfEmpty(Mono.error(new RuntimeException("Invalid email or password")))
        .flatMap(
            user -> {
              if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                return Mono.error(new RuntimeException("Invalid email or password"));
              }

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
            });
  }

  public Mono<AuthResponse> refreshToken(String refreshToken) {
    try {
      if (!jwtUtil.isRefreshToken(refreshToken)) {
        return Mono.error(new RuntimeException("Invalid refresh token"));
      }

      if (jwtUtil.isTokenExpired(refreshToken)) {
        return Mono.error(new RuntimeException("Refresh token expired"));
      }

      UUID userId = jwtUtil.getUserIdFromToken(refreshToken);
      String email = jwtUtil.getEmailFromToken(refreshToken);

      return userRepository
          .findById(userId)
          .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
          .map(
              user -> {
                String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
                String newRefreshToken =
                    jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

                return AuthResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();
              });
    } catch (Exception e) {
      return Mono.error(new RuntimeException("Invalid refresh token", e));
    }
  }
}
