package ru.get.tms.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Authentication Manager для JWT токенов.
 *
 * <p>Просто возвращает переданный Authentication, так как валидация уже произошла в Converter.
 */
@Slf4j
@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

  @Override
  public Mono<Authentication> authenticate(Authentication authentication) {
    // Authentication уже прошел валидацию в JwtAuthenticationConverter
    // Просто возвращаем его как есть
    log.debug("Authenticating user: {}", authentication.getPrincipal());
    return Mono.just(authentication);
  }
}
