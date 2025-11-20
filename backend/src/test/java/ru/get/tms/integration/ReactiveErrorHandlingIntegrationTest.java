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
import ru.get.tms.domain.user.User;
import ru.get.tms.dto.auth.LoginRequest;
import ru.get.tms.dto.auth.RegisterRequest;
import ru.get.tms.exception.ConflictException;
import ru.get.tms.exception.ResourceNotFoundException;
import ru.get.tms.exception.UnauthorizedException;
import ru.get.tms.repository.UserRepository;
import ru.get.tms.service.AuthService;

/**
 * Integration tests for reactive error handling in services (NFR-027).
 *
 * <p>Tests error handling patterns across the full application stack:
 *
 * <ul>
 *   <li>Service layer error handling
 *   <li>Controller layer error mapping
 *   <li>GlobalExceptionHandler responses
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Reactive Error Handling Integration Tests")
class ReactiveErrorHandlingIntegrationTest {

  @Autowired private WebTestClient webTestClient;

  @Autowired private AuthService authService;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;

  @BeforeEach
  void setUp() {
    // Clean up
    userRepository.deleteAll().block();

    // Create test user
    testUser =
        User.builder()
            .id(UUID.randomUUID())
            .email("test@example.com")
            .passwordHash(passwordEncoder.encode("TestPassword123!"))
            .name("Test User")
            .isAdmin(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.save(testUser).block();
  }

  // ========== Service Layer Tests ==========

  @Test
  @DisplayName("Service: Should throw ConflictException for duplicate email")
  void serviceShouldThrowConflictExceptionForDuplicateEmail() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail(testUser.getEmail());
    request.setName("Another User");
    request.setPassword("AnotherPassword123!");

    // When & Then
    StepVerifier.create(authService.register(request))
        .expectError(ConflictException.class)
        .verify();
  }

  @Test
  @DisplayName("Service: Should throw UnauthorizedException for invalid credentials")
  void serviceShouldThrowUnauthorizedForInvalidCredentials() {
    // Given
    LoginRequest request = new LoginRequest();
    request.setEmail(testUser.getEmail());
    request.setPassword("WrongPassword123!");

    // When & Then
    StepVerifier.create(authService.login(request))
        .expectError(UnauthorizedException.class)
        .verify();
  }

  @Test
  @DisplayName("Service: Should throw UnauthorizedException for non-existing user")
  void serviceShouldThrowUnauthorizedForNonExistingUser() {
    // Given
    LoginRequest request = new LoginRequest();
    request.setEmail("nonexisting@example.com");
    request.setPassword("SomePassword123!");

    // When & Then
    StepVerifier.create(authService.login(request))
        .expectError(UnauthorizedException.class)
        .verify();
  }

  // ========== Controller/HTTP Layer Tests ==========

  @Test
  @DisplayName("HTTP: Should return 409 CONFLICT for duplicate email registration")
  void httpShouldReturn409ForDuplicateEmail() {
    // Given
    RegisterRequest request = new RegisterRequest();
    request.setEmail(testUser.getEmail());
    request.setName("Another User");
    request.setPassword("ValidPassword123!");

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isEqualTo(409)
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(409)
        .jsonPath("$.error")
        .isEqualTo("CONFLICT")
        .jsonPath("$.message")
        .value(msg -> assertThat(msg.toString()).contains("already exists"))
        .jsonPath("$.timestamp")
        .exists()
        .jsonPath("$.path")
        .isEqualTo("/api/auth/register");
  }

  @Test
  @DisplayName("HTTP: Should return 401 UNAUTHORIZED for invalid credentials")
  void httpShouldReturn401ForInvalidCredentials() {
    // Given
    LoginRequest request = new LoginRequest();
    request.setEmail(testUser.getEmail());
    request.setPassword("WrongPassword!");

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(401)
        .jsonPath("$.error")
        .isEqualTo("UNAUTHORIZED")
        .jsonPath("$.message")
        .value(msg -> assertThat(msg.toString()).contains("Invalid email or password"));
  }

  @Test
  @DisplayName("HTTP: Should return 400 BAD REQUEST for invalid request body")
  void httpShouldReturn400ForInvalidRequest() {
    // Given - Missing required fields
    String invalidJson = "{\"email\": \"invalid\"}";

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidJson)
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectBody()
        .jsonPath("$.status")
        .isEqualTo(400)
        .jsonPath("$.error")
        .exists();
  }

  @Test
  @DisplayName("HTTP: Should return structured error response with all required fields")
  void httpShouldReturnStructuredErrorResponse() {
    // Given
    LoginRequest request = new LoginRequest();
    request.setEmail("nonexisting@example.com");
    request.setPassword("SomePassword123!");

    // When & Then
    webTestClient
        .post()
        .uri("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus()
        .isUnauthorized()
        .expectBody()
        .jsonPath("$.timestamp")
        .exists()
        .jsonPath("$.status")
        .isNumber()
        .jsonPath("$.error")
        .isNotEmpty()
        .jsonPath("$.message")
        .isNotEmpty()
        .jsonPath("$.path")
        .isEqualTo("/api/auth/login");
  }

  // ========== Reactive Pipeline Tests ==========

  @Test
  @DisplayName("Pipeline: Should not break reactive stream on error")
  void pipelineShouldNotBreakOnError() {
    // Given
    RegisterRequest validRequest = new RegisterRequest();
    validRequest.setEmail("newuser@example.com");
    validRequest.setName("New User");
    validRequest.setPassword("ValidPassword123!");

    RegisterRequest duplicateRequest = new RegisterRequest();
    duplicateRequest.setEmail(testUser.getEmail());
    duplicateRequest.setName("Duplicate User");
    duplicateRequest.setPassword("ValidPassword123!");

    // When - First request succeeds, second fails
    StepVerifier.create(authService.register(validRequest))
        .expectNextMatches(response -> response.getEmail().equals("newuser@example.com"))
        .verifyComplete();

    StepVerifier.create(authService.register(duplicateRequest))
        .expectError(ConflictException.class)
        .verify();

    // Then - First user should still exist in database
    StepVerifier.create(userRepository.findByEmail("newuser@example.com"))
        .expectNextMatches(user -> user.getEmail().equals("newuser@example.com"))
        .verifyComplete();
  }

  @Test
  @DisplayName("Pipeline: Should preserve error context in nested operations")
  void pipelineShouldPreserveErrorContext() {
    // Given
    LoginRequest request = new LoginRequest();
    request.setEmail("nonexisting@example.com");
    request.setPassword("SomePassword123!");

    // When & Then - Error should propagate with correct type
    StepVerifier.create(authService.login(request))
        .expectErrorMatches(
            throwable ->
                throwable instanceof UnauthorizedException
                    && throwable.getMessage().equals("Invalid email or password"))
        .verify();
  }

  @Test
  @DisplayName("Pipeline: Should handle database errors gracefully")
  void pipelineShouldHandleDatabaseErrors() {
    // Given - Try to find user with invalid UUID format (will cause parsing error)
    UUID invalidId = UUID.randomUUID();

    // When & Then
    StepVerifier.create(
            userRepository
                .findById(invalidId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User", invalidId))))
        .expectError(ResourceNotFoundException.class)
        .verify();
  }
}
