package ru.get.tms.domain.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id private UUID id;

  @Column("email")
  private String email;

  @Column("password_hash")
  private String passwordHash;

  @Column("name")
  private String name;

  @Column("avatar_url")
  private String avatarUrl;

  @Column("hourly_rate")
  private BigDecimal hourlyRate;

  @Column("is_admin")
  @Builder.Default
  private Boolean isAdmin = false;

  @Column("notification_preferences")
  private String notificationPreferences; // JSONB as String, will be parsed

  @Column("do_not_disturb_until")
  private LocalDateTime doNotDisturbUntil;

  @Column("timezone")
  @Builder.Default
  private String timezone = "Europe/Moscow";

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
