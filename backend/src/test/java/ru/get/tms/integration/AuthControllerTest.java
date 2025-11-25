package ru.get.tms.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.r2dbc.postgresql.codec.Json;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.user.User;
import ru.get.tms.dto.auth.AuthResponse;
import ru.get.tms.dto.auth.LoginRequest;
import ru.get.tms.dto.auth.RegisterRequest;
import ru.get.tms.repository.UserRepository;

/**
 * Integration tests for AuthController.
 *
 * <p>Tests HTTP endpoints with real database (Testcontainers PostgreSQL). Uses WebTestClient for
 * reactive endpoint testing.
 *
 * @see ru.get.tms.api.rest.AuthController
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("AuthController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("resource")
class AuthControllerTest {

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @Autowired private WebTestClient webTestClient;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  private static final String AUTH_BASE_URL = "/api/auth";
  private static final String TEST_EMAIL = "integration@example.com";
  private static final String TEST_PASSWORD = "Password123!"; // Must meet password requirements
  private static final String TEST_NAME = "Integration Test User";

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () -> postgres.getJdbcUrl().replace("jdbc:", "r2dbc:"));
    registry.add("spring.r2dbc.username", postgres::getUsername);
    registry.add("spring.r2dbc.password", postgres::getPassword);
    registry.add(
        "spring.flyway.url",
        () ->
            String.format(
                "jdbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);
  }

  @BeforeEach
  void setUp() {
    // Clean up database before each test
    userRepository.deleteAll().block();
  }

  // ========== REGISTER ENDPOINT TESTS ==========

  @Test
  @Order(1)
  @DisplayName("POST /api/auth/register - should successfully register new user")
  void registerEndpoint_Success() {
    // Arrange
    RegisterRequest request =
        RegisterRequest.builder().email(TEST_EMAIL).password(TEST_PASSWORD).name(TEST_NAME).build();

    // Act & Assert
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(request), RegisterRequest.class)
        .exchange()
        .expectStatus()
        .isCreated()
        .expectBody(AuthResponse.class)
        .value(
            response -> {
              assertThat(response).isNotNull();
              assertThat(response.getAccessToken()).isNotBlank();
              assertThat(response.getRefreshToken()).isNotBlank();
              assertThat(response.getUserId()).isNotNull();
              assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
              assertThat(response.getName()).isEqualTo(TEST_NAME);
            });

    // Verify user was saved in database
    User savedUser = userRepository.findByEmail(TEST_EMAIL).block();
    assertThat(savedUser).isNotNull();
    assertThat(savedUser.getEmail()).isEqualTo(TEST_EMAIL);
    assertThat(savedUser.getName()).isEqualTo(TEST_NAME);
    assertThat(savedUser.getIsAdmin()).isFalse();
  }

  @Test
  @Order(2)
  @DisplayName("POST /api/auth/register - should return 409 when email already exists")
  void registerEndpoint_EmailExists_ReturnsConflict() {
    // Arrange - Create existing user
    User existingUser =
        User.builder()
            // Don't set id - let database generate it
            .email(TEST_EMAIL)
            .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
            .name("Existing User")
            .isAdmin(false)
            .notificationPreferences(Json.of("{}".getBytes(StandardCharsets.UTF_8)))
            .timezone("Europe/Moscow")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.save(existingUser).block();

    RegisterRequest request =
        RegisterRequest.builder().email(TEST_EMAIL).password(TEST_PASSWORD).name(TEST_NAME).build();

    // Act & Assert
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(request), RegisterRequest.class)
        .exchange()
        .expectStatus()
        .isEqualTo(409); // Conflict
  }

  @Test
  @Order(3)
  @DisplayName("POST /api/auth/register - should return 400 when request is invalid")
  void registerEndpoint_InvalidRequest_ReturnsBadRequest() {
    // Arrange - Request without required fields
    RegisterRequest invalidRequest = RegisterRequest.builder().email(TEST_EMAIL).build();

    // Act & Assert
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/register")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(invalidRequest), RegisterRequest.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  // ========== LOGIN ENDPOINT TESTS ==========

  @Test
  @Order(4)
  @DisplayName("POST /api/auth/login - should successfully login with valid credentials")
  void loginEndpoint_Success() {
    // Arrange - Create user
    User user =
        User.builder()
            // Don't set id - let database generate it
            .email(TEST_EMAIL)
            .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
            .name(TEST_NAME)
            .isAdmin(false)
            .notificationPreferences(Json.of("{}".getBytes(StandardCharsets.UTF_8)))
            .timezone("Europe/Moscow")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

    userRepository.save(user).block();

    LoginRequest request = LoginRequest.builder().email(TEST_EMAIL).password(TEST_PASSWORD).build();

    // Act & Assert
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(request), LoginRequest.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(AuthResponse.class)
        .value(
            response -> {
              assertThat(response).isNotNull();
              assertThat(response.getAccessToken()).isNotBlank();
              assertThat(response.getRefreshToken()).isNotBlank();
              assertThat(response.getUserId()).isEqualTo(user.getId());
              assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            });
  }

  @Test
  @Order(5)
  @DisplayName("POST /api/auth/login - should return 401 when credentials are invalid")
  void loginEndpoint_InvalidCredentials_ReturnsUnauthorized() {
    // Arrange
    LoginRequest request =
        LoginRequest.builder().email(TEST_EMAIL).password("wrongPassword").build();

    // Act & Assert
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(request), LoginRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  @Test
  @Order(6)
  @DisplayName("POST /api/auth/login - should return 401 when user does not exist")
  void loginEndpoint_UserNotFound_ReturnsUnauthorized() {
    // Arrange
    LoginRequest request =
        LoginRequest.builder().email("nonexistent@example.com").password(TEST_PASSWORD).build();

    // Act & Assert
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Mono.just(request), LoginRequest.class)
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }

  // ========== REFRESH TOKEN ENDPOINT TESTS ==========

  @Test
  @Order(7)
  @DisplayName("POST /api/auth/refresh - should successfully refresh token")
  void refreshTokenEndpoint_Success() {
    // Arrange - Register user to get tokens
    RegisterRequest registerRequest =
        RegisterRequest.builder().email(TEST_EMAIL).password(TEST_PASSWORD).name(TEST_NAME).build();

    AuthResponse registerResponse =
        webTestClient
            .post()
            .uri(AUTH_BASE_URL + "/register")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(registerRequest), RegisterRequest.class)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(AuthResponse.class)
            .returnResult()
            .getResponseBody();

    assertThat(registerResponse).isNotNull();
    String refreshToken = registerResponse.getRefreshToken();

    // Act & Assert - Refresh token
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"refreshToken\":\"" + refreshToken + "\"}")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(AuthResponse.class)
        .value(
            response -> {
              assertThat(response).isNotNull();
              assertThat(response.getAccessToken()).isNotBlank();
              assertThat(response.getRefreshToken()).isNotBlank();
              assertThat(response.getEmail()).isEqualTo(TEST_EMAIL);
            });
  }

  @Test
  @Order(8)
  @DisplayName("POST /api/auth/refresh - should return 401 when refresh token is invalid")
  void refreshTokenEndpoint_InvalidToken_ReturnsUnauthorized() {
    // Arrange
    String invalidToken = "invalid.refresh.token";

    // Act & Assert
    webTestClient
        .post()
        .uri(AUTH_BASE_URL + "/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"refreshToken\":\"" + invalidToken + "\"}")
        .exchange()
        .expectStatus()
        .isUnauthorized();
  }
}
