package ru.get.tms.repository;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.user.PasswordResetToken;

/**
 * R2DBC Repository for PasswordResetToken entity.
 *
 * <p>Provides reactive database operations for password reset tokens:
 *
 * <ul>
 *   <li>Find valid tokens by token string
 *   <li>Find tokens by user
 *   <li>Clean up expired tokens
 * </ul>
 *
 * @see PasswordResetToken
 * @see ru.get.tms.service.PasswordResetService
 */
@Repository
public interface PasswordResetTokenRepository
    extends ReactiveCrudRepository<PasswordResetToken, UUID> {

  /**
   * Find a valid token by its string value.
   *
   * <p>Valid token criteria:
   *
   * <ul>
   *   <li>Token string matches
   *   <li>Not expired (expires_at > now)
   *   <li>Not used (used_at is null)
   * </ul>
   *
   * @param token the token string
   * @return Mono of PasswordResetToken if found and valid, empty otherwise
   */
  @Query(
      """
      SELECT * FROM password_reset_tokens
      WHERE token = :token
        AND expires_at > :now
        AND used_at IS NULL
      LIMIT 1
      """)
  Mono<PasswordResetToken> findValidToken(String token, LocalDateTime now);

  /**
   * Find all tokens for a specific user.
   *
   * @param userId the user ID
   * @return Flux of PasswordResetToken
   */
  Flux<PasswordResetToken> findByUserId(UUID userId);

  /**
   * Find all expired or used tokens (for cleanup).
   *
   * @param now current timestamp
   * @return Flux of expired/used tokens
   */
  @Query(
      """
      SELECT * FROM password_reset_tokens
      WHERE expires_at <= :now OR used_at IS NOT NULL
      """)
  Flux<PasswordResetToken> findExpiredOrUsedTokens(LocalDateTime now);

  /**
   * Delete all expired or used tokens (cleanup operation).
   *
   * @param now current timestamp
   * @return Mono of number of deleted tokens
   */
  @Query(
      """
      DELETE FROM password_reset_tokens
      WHERE expires_at <= :now OR used_at IS NOT NULL
      """)
  Mono<Long> deleteExpiredOrUsedTokens(LocalDateTime now);

  /**
   * Invalidate (mark as used) all existing tokens for a user. Useful when user successfully resets
   * password to prevent reuse of old tokens.
   *
   * @param userId the user ID
   * @param now current timestamp
   * @return Mono of number of updated tokens
   */
  @Query(
      """
      UPDATE password_reset_tokens
      SET used_at = :now
      WHERE user_id = :userId AND used_at IS NULL
      """)
  Mono<Long> invalidateUserTokens(UUID userId, LocalDateTime now);
}
