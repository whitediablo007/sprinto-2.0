package ru.get.tms.service;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.user.PasswordResetToken;
import ru.get.tms.repository.PasswordResetTokenRepository;
import ru.get.tms.repository.UserRepository;

/**
 * Service for password reset functionality (FR-007.1).
 *
 * <p>Implements secure password recovery flow:
 *
 * <ol>
 *   <li>Generate unique token with 1-hour expiration
 *   <li>Send reset link via email
 *   <li>Validate token on reset request
 *   <li>Update password and invalidate all user tokens
 * </ol>
 *
 * <p>Security considerations:
 *
 * <ul>
 *   <li>Tokens are single-use (marked as used after password reset)
 *   <li>Tokens expire after configured time (default: 1 hour)
 *   <li>All user tokens are invalidated after successful password reset
 *   <li>No user enumeration - same response for existing/non-existing emails
 * </ul>
 *
 * @see PasswordResetToken
 * @see EmailService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private final PasswordResetTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final EmailService emailService;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.password-reset.token-validity-hours}")
  private int tokenValidityHours;

  /**
   * Generate password reset token and send email.
   *
   * <p>Note: Always returns success to prevent user enumeration. If email doesn't exist, no email
   * is sent but response is identical.
   *
   * @param email user email
   * @return Mono&lt;Void&gt; that completes when process finishes
   */
  @Transactional
  public Mono<Void> requestPasswordReset(String email) {
    log.info("Password reset requested for email: {}", email);

    return userRepository
        .findByEmail(email)
        .flatMap(
            user -> {
              // Generate token
              String tokenValue = UUID.randomUUID().toString();
              LocalDateTime now = LocalDateTime.now();

              PasswordResetToken token =
                  PasswordResetToken.builder()
                      .id(UUID.randomUUID())
                      .userId(user.getId())
                      .token(tokenValue)
                      .expiresAt(now.plusHours(tokenValidityHours))
                      .createdAt(now)
                      .build();

              // Save token and send email
              return tokenRepository
                  .save(token)
                  .flatMap(
                      savedToken -> {
                        log.info(
                            "Password reset token generated for user: {} (expires: {})",
                            user.getEmail(),
                            savedToken.getExpiresAt());
                        return emailService.sendPasswordResetEmail(
                            user.getEmail(), user.getName(), tokenValue);
                      });
            })
        .doOnSuccess(
            v ->
                log.info(
                    "Password reset process completed for email: {} (email sent if user exists)",
                    email))
        .onErrorResume(
            e -> {
              log.error("Error during password reset request for email: {}", email, e);
              // Return success to prevent user enumeration
              return Mono.empty();
            })
        .then();
  }

  /**
   * Validate password reset token.
   *
   * @param token reset token
   * @return Mono&lt;PasswordResetToken&gt; if token is valid
   */
  public Mono<PasswordResetToken> validateToken(String token) {
    log.debug("Validating password reset token");

    return tokenRepository
        .findValidToken(token, LocalDateTime.now())
        .switchIfEmpty(
            Mono.error(
                new IllegalArgumentException(
                    "Invalid or expired token. Please request a new password reset.")));
  }

  /**
   * Reset user password using valid token.
   *
   * @param token reset token
   * @param newPassword new password (plain text, will be encoded)
   * @return Mono&lt;Void&gt; that completes when password is reset
   */
  @Transactional
  public Mono<Void> resetPassword(String token, String newPassword) {
    log.info("Attempting password reset with token");

    return validateToken(token)
        .flatMap(
            resetToken ->
                userRepository
                    .findById(resetToken.getUserId())
                    .switchIfEmpty(Mono.error(new IllegalStateException("User not found")))
                    .flatMap(
                        user -> {
                          // Update password
                          user.setPasswordHash(passwordEncoder.encode(newPassword));
                          user.setUpdatedAt(LocalDateTime.now());

                          return userRepository
                              .save(user)
                              .flatMap(
                                  savedUser -> {
                                    log.info(
                                        "Password successfully reset for user: {}",
                                        savedUser.getEmail());

                                    // Mark token as used
                                    resetToken.markAsUsed();

                                    // Invalidate all user tokens and mark current as used
                                    return tokenRepository
                                        .invalidateUserTokens(user.getId(), LocalDateTime.now())
                                        .then(tokenRepository.save(resetToken))
                                        .then();
                                  });
                        }))
        .doOnError(e -> log.error("Failed to reset password", e));
  }

  /**
   * Cleanup expired and used tokens. Should be scheduled to run periodically (e.g., daily).
   *
   * @return Mono&lt;Long&gt; number of deleted tokens
   */
  @Transactional
  public Mono<Long> cleanupExpiredTokens() {
    log.info("Running password reset tokens cleanup");

    return tokenRepository
        .deleteExpiredOrUsedTokens(LocalDateTime.now())
        .doOnSuccess(count -> log.info("Cleaned up {} expired/used password reset tokens", count))
        .doOnError(e -> log.error("Failed to cleanup expired tokens", e));
  }
}
