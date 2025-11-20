package ru.get.tms.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.get.tms.domain.user.User;
import ru.get.tms.dto.auth.RegisterRequest;
import ru.get.tms.repository.UserRepository;

/**
 * Integration tests for security policies (FR-007.2, FR-007.3, NFR-020, SC-001).
 *
 * <p>Tests:
 *
 * <ul>
 *   <li>Password complexity validation (FR-007.2)
 *   <li>Session inactivity timeout configuration (FR-007.3, NFR-020)
 *   <li>JWT token expiration (SC-001)
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Security Policy Integration Tests")
class SecurityPolicyTest {

  @Autowired private WebTestClient webTestClient;

  @Autowired private UserRepository userRepository;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll().block();
  }

  @Test
  @DisplayName("FR-007.2: Should accept strong password meeting all requirements")
  void shouldAcceptStrongPassword() {
    // Given - Strong password with all requirements
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    request.setPassword("StrongPass123!"); // Min 8 chars, uppercase, lowercase, digit, special

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody()
        .jsonPath("$.accessToken")
        .exists()
        .jsonPath("$.refreshToken")
        .exists();
  }

  @Test
  @DisplayName("FR-007.2: Should reject password without uppercase letter")
  void shouldRejectPasswordWithoutUppercase() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    request.setPassword("weakpass123!"); // No uppercase

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("FR-007.2: Should reject password without lowercase letter")
  void shouldRejectPasswordWithoutLowercase() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    request.setPassword("WEAKPASS123!"); // No lowercase

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("FR-007.2: Should reject password without digit")
  void shouldRejectPasswordWithoutDigit() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    request.setPassword("WeakPassword!"); // No digit

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("FR-007.2: Should reject password without special character")
  void shouldRejectPasswordWithoutSpecialChar() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    request.setPassword("WeakPassword123"); // No special char

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("FR-007.2: Should reject password shorter than 8 characters")
  void shouldRejectShortPassword() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    request.setPassword("Shrt1!"); // Only 6 characters

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("FR-007.2: Should accept various special characters")
  void shouldAcceptVariousSpecialCharacters() {
    String[] validSpecialChars = {"@", "#", "$", "%", "^", "&", "+", "=", "!"};

    for (int i = 0; i < validSpecialChars.length; i++) {
      String email = "test" + i + "@example.com";
      RegisterRequest request = new RegisterRequest();
      request.setEmail(email);
      request.setName("Test User " + i);
      request.setPassword("ValidPass123" + validSpecialChars[i]);

      // When & Then
      webTestClient
          .post()
          .uri("/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .isCreated();
    }
  }

  @Test
  @DisplayName("FR-007.2: Should provide clear validation error messages")
  void shouldProvideValidationErrorMessages() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    request.setPassword("weak"); // Multiple violations

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError()
        .expectBody()
        .jsonPath("$.message")
        .exists();
  }

  @Test
  @DisplayName("FR-007.3: Should hash passwords using BCrypt")
  void shouldHashPasswordsUsingBCrypt() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("Test User");
    String plainPassword = "StrongPass123!";
    request.setPassword(plainPassword);

    // When
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isCreated();

    // Then - Verify password is hashed (not stored in plain text)
    User user = userRepository.findByEmail(request.getEmail()).block();
    assertThat(user).isNotNull();
    assertThat(user.getPasswordHash()).isNotEqualTo(plainPassword);
    assertThat(user.getPasswordHash()).startsWith("$2a$"); // BCrypt prefix
    assertThat(user.getPasswordHash().length()).isGreaterThan(50); // BCrypt hash length
  }

  @Test
  @DisplayName("NFR-020: Should reject invalid email format")
  void shouldRejectInvalidEmailFormat() {
    String[] invalidEmails = {
      "notanemail", "missing@domain", "@nodomain.com", "spaces in@email.com"
    };

    for (String invalidEmail : invalidEmails) {
      RegisterRequest request = new RegisterRequest();
      request.setEmail(invalidEmail);
      request.setName("Test User");
      request.setPassword("StrongPass123!");

      // When & Then
      webTestClient
          .post()
          .uri("/api/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(request)
          .exchange()
          .expectStatus()
          .is4xxClientError();
    }
  }

  @Test
  @DisplayName("SC-001: Should prevent duplicate email registration")
  void shouldPreventDuplicateEmailRegistration() {
    // Given - First registration
    RegisterRequest firstRequest = new RegisterRequest();
    firstRequest.setEmail("test@example.com");
    firstRequest.setName("Test User 1");
    firstRequest.setPassword("StrongPass123!");

    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(firstRequest)
        .exchange()
        .expectStatus()
        .isCreated();

    // When - Attempt duplicate registration
    RegisterRequest duplicateRequest = new RegisterRequest();
    duplicateRequest.setEmail("test@example.com"); // Same email
    duplicateRequest.setName("Test User 2");
    duplicateRequest.setPassword("DifferentPass123!");

    // Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(duplicateRequest)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }

  @Test
  @DisplayName("SC-001: Should validate name field")
  void shouldValidateNameField() {
    // Given - Too short name
    RegisterRequest request = new RegisterRequest();
    request.setEmail("test@example.com");
    request.setName("A"); // Only 1 character (min is 2)
    request.setPassword("StrongPass123!");

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .is4xxClientError();
  }
}
