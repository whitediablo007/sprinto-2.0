package ru.get.tms.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for password reset confirmation (FR-007.1, FR-007.2).
 *
 * <p>Used when user confirms password reset with token and new password.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirm {

  @NotBlank(message = "Token is required")
  private String token;

  @NotBlank(message = "New password is required")
  @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
  @Pattern(
      regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
      message =
          "Password must contain at least one digit, one lowercase letter, one uppercase letter,"
              + " and one special character (@#$%^&+=!)")
  private String newPassword;
}
