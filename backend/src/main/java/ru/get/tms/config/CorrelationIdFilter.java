package ru.get.tms.config;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * WebFlux filter for Correlation ID management (NFR-038 to NFR-043).
 *
 * <p>This filter:
 *
 * <ul>
 *   <li>Extracts Correlation ID from incoming request header (X-Correlation-ID)
 *   <li>Generates new UUID if header is missing
 *   <li>Stores Correlation ID in Reactor Context for reactive propagation
 *   <li>Adds Correlation ID to response headers
 *   <li>Populates SLF4J MDC for structured logging
 * </ul>
 *
 * <p>The Correlation ID is automatically included in all logs via Logback MDC configuration.
 *
 * @see ru.get.tms.exception.GlobalExceptionHandler
 */
@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter implements WebFilter {

  public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  public static final String CORRELATION_ID_KEY = "correlationId";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Extract or generate Correlation ID
    String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);

    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }

    // Add to response headers
    exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

    // Store in Reactor Context for reactive chain propagation
    final String finalCorrelationId = correlationId;

    return chain
        .filter(exchange)
        .contextWrite(Context.of(CORRELATION_ID_KEY, finalCorrelationId))
        .doOnEach(
            signal -> {
              // Populate MDC from Reactor Context for logging
              signal
                  .getContextView()
                  .getOrEmpty(CORRELATION_ID_KEY)
                  .ifPresent(id -> MDC.put(CORRELATION_ID_KEY, id.toString()));
            })
        .doFinally(
            signalType -> {
              // Clean up MDC after request processing
              MDC.remove(CORRELATION_ID_KEY);
            });
  }
}
