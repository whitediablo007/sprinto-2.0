package ru.get.tms.security;

import java.util.Collections;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Конвертер для извлечения JWT токена из запроса и создания Authentication.
 *
 * <p>Извлекает JWT из заголовка Authorization и создает Authentication объект с userId в качестве
 * principal.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter implements ServerAuthenticationConverter {

  private final JwtUtil jwtUtil;

  @Override
  public Mono<Authentication> convert(ServerWebExchange exchange) {
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    log.info(
        "JWT Converter - Authorization header: {}",
        authHeader != null
            ? authHeader.substring(0, Math.min(50, authHeader.length())) + "..."
            : "null");

    return Mono.justOrEmpty(authHeader)
        .filter(
            header -> {
              boolean valid = header.startsWith("Bearer ");
              log.info("JWT Converter - Header starts with Bearer: {}", valid);
              return valid;
            })
        .map(header -> header.substring(7))
        .filter(
            token -> {
              boolean notBlank = !token.isBlank();
              log.info("JWT Converter - Token not blank: {}", notBlank);
              return notBlank;
            })
        .flatMap(
            token -> {
              try {
                log.info("JWT Converter - Validating token...");

                // Валидируем токен
                if (jwtUtil.isTokenExpired(token)) {
                  log.warn("JWT Converter - Token is EXPIRED");
                  return Mono.empty();
                }
                log.info("JWT Converter - Token is NOT expired");

                // Извлекаем userId из токена
                UUID userId = jwtUtil.getUserIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);

                log.info(
                    "JWT Converter - Authentication successful for userId: {}, email: {}",
                    userId,
                    email);

                // Создаем аутентифицированный Authentication
                // ВАЖНО: конструктор с authorities автоматически делает authenticated=true
                // НЕ вызывать setAuthenticated(true) - это бросает IllegalArgumentException
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                        userId.toString(), // principal = userId string
                        null, // credentials = null (токен не храним в credentials)
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                // auth уже authenticated благодаря конструктору с authorities

                return Mono.just((Authentication) auth);
              } catch (Exception e) {
                log.error("JWT Converter - Failed to parse token: {}", e.getMessage(), e);
                return Mono.empty();
              }
            });
  }
}
