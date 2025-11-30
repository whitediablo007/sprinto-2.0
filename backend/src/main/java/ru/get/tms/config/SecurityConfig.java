package ru.get.tms.config;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.web.cors.CorsConfiguration;
import ru.get.tms.security.JwtAuthenticationConverter;
import ru.get.tms.security.JwtAuthenticationManager;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationManager jwtAuthenticationManager;
  private final JwtAuthenticationConverter jwtAuthenticationConverter;

  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    // Создаем JWT authentication filter
    AuthenticationWebFilter jwtAuthenticationFilter =
        new AuthenticationWebFilter(jwtAuthenticationManager);
    jwtAuthenticationFilter.setServerAuthenticationConverter(jwtAuthenticationConverter);

    // Фильтр должен обрабатывать только запросы с Authorization header
    // Запросы без токена будут отклонены через authorizeExchange
    jwtAuthenticationFilter.setRequiresAuthenticationMatcher(
        exchange -> {
          boolean hasAuthHeader =
              exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION);
          return hasAuthHeader
              ? ServerWebExchangeMatcher.MatchResult.match()
              : ServerWebExchangeMatcher.MatchResult.notMatch();
        });

    return http.cors(
            cors ->
                cors.configurationSource(
                    request -> {
                      CorsConfiguration config = new CorsConfiguration();
                      config.setAllowedOrigins(List.of("http://localhost:4200"));
                      config.setAllowedMethods(
                          Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                      config.setAllowedHeaders(List.of("*"));
                      config.setAllowCredentials(true);
                      config.setMaxAge(3600L);
                      return config;
                    }))
        .csrf(csrf -> csrf.disable())
        .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
        .authorizeExchange(
            exchanges ->
                exchanges
                    .pathMatchers("/api/auth/**")
                    .permitAll()
                    .pathMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**")
                    .permitAll()
                    .pathMatchers("/ws/**")
                    .permitAll() // WebSocket будет защищен через JWT в handler
                    .pathMatchers(HttpMethod.OPTIONS)
                    .permitAll()
                    .anyExchange()
                    .authenticated())
        .httpBasic(httpBasic -> httpBasic.disable())
        .formLogin(formLogin -> formLogin.disable())
        .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }
}
