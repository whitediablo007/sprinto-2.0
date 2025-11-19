package ru.get.tms.dto.auth;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

  private UUID userId;
  private String email;
  private String name;
  private String accessToken;
  private String refreshToken;

  @Builder.Default private String tokenType = "Bearer";
}
