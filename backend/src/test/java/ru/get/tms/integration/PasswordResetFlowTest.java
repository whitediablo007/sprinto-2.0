package ru.get.tms.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import ru.get.tms.domain.user.PasswordResetToken;
import ru.get.tms.domain.user.User;
import ru.get.tms.dto.auth.PasswordResetConfirm;
import ru.get.tms.dto.auth.PasswordResetRequest;
import ru.get.tms.repository.PasswordResetTokenRepository;
import ru.get.tms.repository.UserRepository;

/**
 * Integration tests for password reset flow (FR-007.1).
 *
 * <p>Tests the complete password recovery process:
 *
 * <ul>
 *   <li>Request password reset (generate token and send email)
 *   <li>Validate token
 *   <li>Reset password with valid token
 *   <li>Reject expired or used tokens
 *   <li>Invalidate old tokens after password reset
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Password Reset Flow Integration Tests")
class PasswordResetFlowTest {

  @Autowired private WebTestClient webTestClient;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordResetTokenRepository tokenRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;
  private String testEmail = "test@example.com";
  private String testPassword = "OldPassword123!";

  @BeforeEach
  void setUp() {
    // Clean up
    tokenRepository.deleteAll().block();
    userRepository.deleteAll().block();

    // Create test user
    testUser =
        User.builder()
            .id(UUID.randomUUID())
            .email(testEmail)
            .passwordHash(passwordEncoder.encode(testPassword))
            .name("Test User")
            .isAdmin(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.save(testUser).block();
  }

  @Test
  @DisplayName("Should successfully request password reset for existing user")
  void shouldRequestPasswordReset() {
    // Given
    PasswordResetRequest request = new PasswordResetRequest();
    request.setEmail(testEmail);

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/password-reset/request")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk();

    // Verify token was created
    StepVerifier.create(tokenRepository.findByUserId(testUser.getId()))
        .assertNext(
            token -> {
              assertThat(token.getUserId()).isEqualTo(testUser.getId());
              assertThat(token.getToken()).isNotNull();
              assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now());
              assertThat(token.getUsedAt()).isNull();
            })
        .verifyComplete();
  }

  @Test
  @DisplayName("Should return success even for non-existing email (prevent enumeration)")
  void shouldNotExposeUserExistence() {
    // Given
    PasswordResetRequest request = new PasswordResetRequest();
    request.setEmail("nonexisting@example.com");

    // When & Then - Should return 200 even though user doesn't exist
    webTestClient
        .post()
        .uri("/api/auth/password-reset/request")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isOk();

    // Verify no token was created
    StepVerifier.create(tokenRepository.findAll()).verifyComplete();
  }

  @Test
  @DisplayName("Should successfully reset password with valid token")
  void shouldResetPasswordWithValidToken() {
    // Given - Create password reset token
    String tokenValue = UUID.randomUUID().toString();
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(UUID.randomUUID())
            .userId(testUser.getId())
            .token(tokenValue)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .createdAt(LocalDateTime.now())
            .build();
    tokenRepository.save(token).block();

    String newPassword = "NewPassword123!";
    PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
    confirmRequest.setToken(tokenValue);
    confirmRequest.setNewPassword(newPassword);

    // When
    webTestClient
        .post()
        .uri("/api/auth/password-reset/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(confirmRequest)
        .exchange()
        .expectStatus()
        .isOk();

    // Then - Verify password was changed
    User updatedUser = userRepository.findById(testUser.getId()).block();
    assertThat(updatedUser).isNotNull();
    assertThat(passwordEncoder.matches(newPassword, updatedUser.getPasswordHash())).isTrue();

    // Verify token was marked as used
    PasswordResetToken usedToken = tokenRepository.findById(token.getId()).block();
    assertThat(usedToken).isNotNull();
    assertThat(usedToken.getUsedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should reject expired token")
  void shouldRejectExpiredToken() {
    // Given - Create expired token
    String tokenValue = UUID.randomUUID().toString();
    PasswordResetToken expiredToken =
        PasswordResetToken.builder()
            .id(UUID.randomUUID())
            .userId(testUser.getId())
            .token(tokenValue)
            .expiresAt(LocalDateTime.now().minusHours(1)) // Expired 1 hour ago
            .createdAt(LocalDateTime.now().minusHours(2))
            .build();
    tokenRepository.save(expiredToken).block();

    PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
    confirmRequest.setToken(tokenValue);
    confirmRequest.setNewPassword("NewPassword123!");

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/password-reset/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(confirmRequest)
        .exchange()
        .expectStatus()
        .is4xxClientError();

    // Verify password was NOT changed
    User unchangedUser = userRepository.findById(testUser.getId()).block();
    assertThat(unchangedUser).isNotNull();
    assertThat(passwordEncoder.matches(testPassword, unchangedUser.getPasswordHash())).isTrue();
  }

  @Test
  @DisplayName("Should reject already used token")
  void shouldRejectUsedToken() {
    // Given - Create used token
    String tokenValue = UUID.randomUUID().toString();
    PasswordResetToken usedToken =
        PasswordResetToken.builder()
            .id(UUID.randomUUID())
            .userId(testUser.getId())
            .token(tokenValue)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .usedAt(LocalDateTime.now().minusMinutes(30)) // Already used
            .createdAt(LocalDateTime.now().minusHours(1))
            .build();
    tokenRepository.save(usedToken).block();

    PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
    confirmRequest.setToken(tokenValue);
    confirmRequest.setNewPassword("NewPassword123!");

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/password-reset/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(confirmRequest)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("Should reject invalid token")
  void shouldRejectInvalidToken() {
    // Given
    PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
    confirmRequest.setToken("invalid-token-that-does-not-exist");
    confirmRequest.setNewPassword("NewPassword123!");

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/password-reset/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(confirmRequest)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("Should invalidate all user tokens after successful password reset")
  void shouldInvalidateAllTokensAfterReset() {
    // Given - Create multiple tokens for user
    String validTokenValue = UUID.randomUUID().toString();
    PasswordResetToken validToken =
        PasswordResetToken.builder()
            .id(UUID.randomUUID())
            .userId(testUser.getId())
            .token(validTokenValue)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .createdAt(LocalDateTime.now())
            .build();

    PasswordResetToken oldToken =
        PasswordResetToken.builder()
            .id(UUID.randomUUID())
            .userId(testUser.getId())
            .token(UUID.randomUUID().toString())
            .expiresAt(LocalDateTime.now().plusHours(1))
            .createdAt(LocalDateTime.now().minusMinutes(30))
            .build();

    tokenRepository.save(validToken).block();
    tokenRepository.save(oldToken).block();

    // When - Reset password
    PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
    confirmRequest.setToken(validTokenValue);
    confirmRequest.setNewPassword("NewPassword123!");

    webTestClient
        .post()
        .uri("/api/auth/password-reset/confirm")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(confirmRequest)
        .exchange()
        .expectStatus()
        .isOk();

    // Then - Verify all tokens are marked as used
    StepVerifier.create(tokenRepository.findByUserId(testUser.getId()))
        .assertNext(token -> assertThat(token.getUsedAt()).isNotNull())
        .assertNext(token -> assertThat(token.getUsedAt()).isNotNull())
        .verifyComplete();
  }

  @Test
  @DisplayName("Should reject weak password (FR-007.2)")
  void shouldRejectWeakPassword() {
    // Given
    String tokenValue = UUID.randomUUID().toString();
    PasswordResetToken token =
        PasswordResetToken.builder()
            .id(UUID.randomUUID())
            .userId(testUser.getId())
            .token(tokenValue)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .createdAt(LocalDateTime.now())
            .build();
    tokenRepository.save(token).block();

    // Test various weak passwords
    String[] weakPasswords = {
      "short", // Too short
      "alllowercase123!", // No uppercase
      "ALLUPPERCASE123!", // No lowercase
      "NoDigitsHere!", // No digits
      "NoSpecialChar123" // No special character
    };

    for (String weakPassword : weakPasswords) {
      PasswordResetConfirm confirmRequest = new PasswordResetConfirm();
      confirmRequest.setToken(tokenValue);
      confirmRequest.setNewPassword(weakPassword);

      // When & Then
      webTestClient
          .post()
          .uri("/api/auth/password-reset/confirm")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(confirmRequest)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }
}
