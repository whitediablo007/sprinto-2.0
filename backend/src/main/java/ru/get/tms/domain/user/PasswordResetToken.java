package ru.get.tms.domain.user;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Password Reset Token entity. Represents temporary tokens for password recovery process
 * (FR-007.1).
 *
 * <p>Token lifecycle:
 *
 * <ul>
 *   <li>Created when user requests password reset
 *   <li>Valid for 1 hour from creation
 *   <li>Can be used only once (used_at becomes non-null after usage)
 *   <li>Automatically invalidated after expiration
 * </ul>
 *
 * @see ru.get.tms.service.PasswordResetService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("password_reset_tokens")
public class PasswordResetToken {

  @Id
  @Column("id")
  private UUID id;

  @Column("user_id")
  private UUID userId;

  @Column("token")
  private String token;

  @Column("expires_at")
  private LocalDateTime expiresAt;

  @Column("used_at")
  private LocalDateTime usedAt;

  @Column("created_at")
  private LocalDateTime createdAt;

  /**
   * Checks if the token is valid (not expired and not used).
   *
   * @return true if token is valid, false otherwise
   */
  public boolean isValid() {
    return usedAt == null && expiresAt.isAfter(LocalDateTime.now());
  }

  /** Marks the token as used. */
  public void markAsUsed() {
    this.usedAt = LocalDateTime.now();
  }
}
