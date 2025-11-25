package ru.get.tms.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.get.tms.security.JwtUtil;

/**
 * Unit tests for JwtUtil.
 *
 * <p>Tests JWT token generation, validation, and parsing logic. Uses real JwtUtil instance with
 * test configuration.
 *
 * @see ru.get.tms.security.JwtUtil
 */
@DisplayName("JwtUtil Unit Tests")
class JwtUtilTest {

  private JwtUtil jwtUtil;

  private static final String TEST_SECRET =
      "test-secret-key-for-jwt-must-be-at-least-256-bits-long-for-hs256-algorithm";
  private static final long ACCESS_TOKEN_EXPIRATION = 900000L; // 15 minutes
  private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7 days

  private UUID testUserId;
  private String testEmail;

  @BeforeEach
  void setUp() {
    jwtUtil = new JwtUtil(TEST_SECRET, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION);
    testUserId = UUID.randomUUID();
    testEmail = "test@example.com";
  }

  // ========== ACCESS TOKEN TESTS ==========

  @Test
  @DisplayName("generateAccessToken() - should generate valid access token")
  void generateAccessToken_Success() {
    // Act
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Assert
    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature

    // Verify token contains expected claims
    UUID extractedUserId = jwtUtil.getUserIdFromToken(token);
    String extractedEmail = jwtUtil.getEmailFromToken(token);

    assertThat(extractedUserId).isEqualTo(testUserId);
    assertThat(extractedEmail).isEqualTo(testEmail);
  }

  @Test
  @DisplayName("generateAccessToken() - should not be marked as refresh token")
  void generateAccessToken_ShouldNotBeRefreshToken() {
    // Act
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Assert
    assertThat(jwtUtil.isRefreshToken(token)).isFalse();
  }

  @Test
  @DisplayName("generateAccessToken() - should have correct expiration time")
  void generateAccessToken_HasCorrectExpiration() {
    // Act
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Assert
    Date expiration = jwtUtil.getExpirationDateFromToken(token);
    long tokenDuration = expiration.getTime() - new Date().getTime();

    // Should be approximately 15 minutes (with 10 second tolerance)
    assertThat(tokenDuration).isBetween(ACCESS_TOKEN_EXPIRATION - 10000, ACCESS_TOKEN_EXPIRATION);
  }

  // ========== REFRESH TOKEN TESTS ==========

  @Test
  @DisplayName("generateRefreshToken() - should generate valid refresh token")
  void generateRefreshToken_Success() {
    // Act
    String token = jwtUtil.generateRefreshToken(testUserId, testEmail);

    // Assert
    assertThat(token).isNotBlank();
    assertThat(token.split("\\.")).hasSize(3);

    // Verify token contains expected claims
    UUID extractedUserId = jwtUtil.getUserIdFromToken(token);
    String extractedEmail = jwtUtil.getEmailFromToken(token);

    assertThat(extractedUserId).isEqualTo(testUserId);
    assertThat(extractedEmail).isEqualTo(testEmail);
  }

  @Test
  @DisplayName("generateRefreshToken() - should be marked as refresh token")
  void generateRefreshToken_ShouldBeMarkedAsRefreshToken() {
    // Act
    String token = jwtUtil.generateRefreshToken(testUserId, testEmail);

    // Assert
    assertThat(jwtUtil.isRefreshToken(token)).isTrue();
  }

  @Test
  @DisplayName("generateRefreshToken() - should have correct expiration time")
  void generateRefreshToken_HasCorrectExpiration() {
    // Act
    String token = jwtUtil.generateRefreshToken(testUserId, testEmail);

    // Assert
    Date expiration = jwtUtil.getExpirationDateFromToken(token);
    long tokenDuration = expiration.getTime() - new Date().getTime();

    // Should be approximately 7 days (with 10 second tolerance)
    assertThat(tokenDuration).isBetween(REFRESH_TOKEN_EXPIRATION - 10000, REFRESH_TOKEN_EXPIRATION);
  }

  // ========== TOKEN EXTRACTION TESTS ==========

  @Test
  @DisplayName("getUserIdFromToken() - should extract user ID correctly")
  void getUserIdFromToken_Success() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    UUID extractedUserId = jwtUtil.getUserIdFromToken(token);

    // Assert
    assertThat(extractedUserId).isEqualTo(testUserId);
  }

  @Test
  @DisplayName("getEmailFromToken() - should extract email correctly")
  void getEmailFromToken_Success() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    String extractedEmail = jwtUtil.getEmailFromToken(token);

    // Assert
    assertThat(extractedEmail).isEqualTo(testEmail);
  }

  @Test
  @DisplayName("getExpirationDateFromToken() - should extract expiration date correctly")
  void getExpirationDateFromToken_Success() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    Date expiration = jwtUtil.getExpirationDateFromToken(token);

    // Assert
    assertThat(expiration).isAfter(new Date());
    assertThat(expiration)
        .isBefore(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION + 1000));
  }

  @Test
  @DisplayName("extractClaim() - should extract custom claim correctly")
  void extractClaim_Success() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    String subject = jwtUtil.extractClaim(token, Claims::getSubject);

    // Assert
    assertThat(subject).isEqualTo(testEmail);
  }

  // ========== TOKEN VALIDATION TESTS ==========

  @Test
  @DisplayName("validateToken() - should return true for valid token")
  void validateToken_ValidToken_ReturnsTrue() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    Boolean isValid = jwtUtil.validateToken(token, testEmail);

    // Assert
    assertThat(isValid).isTrue();
  }

  @Test
  @DisplayName("validateToken() - should return false for token with wrong email")
  void validateToken_WrongEmail_ReturnsFalse() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    Boolean isValid = jwtUtil.validateToken(token, "wrong@example.com");

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("validateToken() - should return false for invalid token")
  void validateToken_InvalidToken_ReturnsFalse() {
    // Arrange
    String invalidToken = "invalid.jwt.token";

    // Act
    Boolean isValid = jwtUtil.validateToken(invalidToken, testEmail);

    // Assert
    assertThat(isValid).isFalse();
  }

  @Test
  @DisplayName("isTokenExpired() - should return false for valid token")
  void isTokenExpired_ValidToken_ReturnsFalse() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    Boolean isExpired = jwtUtil.isTokenExpired(token);

    // Assert
    assertThat(isExpired).isFalse();
  }

  @Test
  @DisplayName("isTokenExpired() - should return true for expired token")
  void isTokenExpired_ExpiredToken_ReturnsTrue() {
    // Arrange - Create JwtUtil with very short expiration (1ms)
    JwtUtil shortLivedJwtUtil = new JwtUtil(TEST_SECRET, 1L, 1L);
    String token = shortLivedJwtUtil.generateAccessToken(testUserId, testEmail);

    // Wait for token to expire
    try {
      TimeUnit.MILLISECONDS.sleep(10);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Act
    Boolean isExpired = shortLivedJwtUtil.isTokenExpired(token);

    // Assert
    assertThat(isExpired).isTrue();
  }

  @Test
  @DisplayName("isTokenExpired() - should return true for invalid token")
  void isTokenExpired_InvalidToken_ReturnsTrue() {
    // Arrange
    String invalidToken = "invalid.jwt.token";

    // Act
    Boolean isExpired = jwtUtil.isTokenExpired(invalidToken);

    // Assert
    assertThat(isExpired).isTrue();
  }

  // ========== REFRESH TOKEN DETECTION TESTS ==========

  @Test
  @DisplayName("isRefreshToken() - should return true for refresh token")
  void isRefreshToken_RefreshToken_ReturnsTrue() {
    // Arrange
    String token = jwtUtil.generateRefreshToken(testUserId, testEmail);

    // Act
    Boolean isRefresh = jwtUtil.isRefreshToken(token);

    // Assert
    assertThat(isRefresh).isTrue();
  }

  @Test
  @DisplayName("isRefreshToken() - should return false for access token")
  void isRefreshToken_AccessToken_ReturnsFalse() {
    // Arrange
    String token = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Act
    Boolean isRefresh = jwtUtil.isRefreshToken(token);

    // Assert
    assertThat(isRefresh).isFalse();
  }

  @Test
  @DisplayName("isRefreshToken() - should return false for invalid token")
  void isRefreshToken_InvalidToken_ReturnsFalse() {
    // Arrange
    String invalidToken = "invalid.jwt.token";

    // Act
    Boolean isRefresh = jwtUtil.isRefreshToken(invalidToken);

    // Assert
    assertThat(isRefresh).isFalse();
  }

  // ========== ERROR HANDLING TESTS ==========

  @Test
  @DisplayName("getUserIdFromToken() - should throw exception for invalid token")
  void getUserIdFromToken_InvalidToken_ThrowsException() {
    // Arrange
    String invalidToken = "invalid.jwt.token";

    // Act & Assert
    assertThatThrownBy(() -> jwtUtil.getUserIdFromToken(invalidToken))
        .isInstanceOf(Exception.class);
  }

  @Test
  @DisplayName("getEmailFromToken() - should throw exception for invalid token")
  void getEmailFromToken_InvalidToken_ThrowsException() {
    // Arrange
    String invalidToken = "invalid.jwt.token";

    // Act & Assert
    assertThatThrownBy(() -> jwtUtil.getEmailFromToken(invalidToken)).isInstanceOf(Exception.class);
  }

  // ========== TOKEN INDEPENDENCE TESTS ==========

  @Test
  @DisplayName("generateAccessToken() - should generate different tokens for different users")
  void generateAccessToken_DifferentUsers_GeneratesDifferentTokens() {
    // Arrange
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    String email1 = "user1@example.com";
    String email2 = "user2@example.com";

    // Act
    String token1 = jwtUtil.generateAccessToken(userId1, email1);
    String token2 = jwtUtil.generateAccessToken(userId2, email2);

    // Assert
    assertThat(token1).isNotEqualTo(token2);
    assertThat(jwtUtil.getUserIdFromToken(token1)).isEqualTo(userId1);
    assertThat(jwtUtil.getUserIdFromToken(token2)).isEqualTo(userId2);
  }

  @Test
  @DisplayName(
      "generateAccessToken() - should generate different tokens for same user at different times")
  void generateAccessToken_SameUserDifferentTimes_GeneratesDifferentTokens() {
    // Act
    String token1 = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Wait for at least 1 second to ensure different iat (issued at) timestamp
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    String token2 = jwtUtil.generateAccessToken(testUserId, testEmail);

    // Assert
    assertThat(token1).isNotEqualTo(token2); // Different issued_at times
    assertThat(jwtUtil.getUserIdFromToken(token1)).isEqualTo(testUserId);
    assertThat(jwtUtil.getUserIdFromToken(token2)).isEqualTo(testUserId);
  }
}
