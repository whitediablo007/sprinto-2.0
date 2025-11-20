package ru.get.tms.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for password reset request (FR-007.1).
 *
 * <p>Used when user requests a password reset link.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Email must be valid")
  private String email;
}
