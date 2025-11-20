package ru.get.tms.exception;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global exception handler for WebFlux controllers (NFR-027).
 *
 * <p>Handles all exceptions in reactive controllers and maps them to appropriate HTTP responses:
 *
 * <ul>
 *   <li>Domain exceptions → HTTP status from exception
 *   <li>Validation errors → 400 BAD REQUEST
 *   <li>Unexpected errors → 500 INTERNAL SERVER ERROR
 * </ul>
 *
 * <p>All error responses include:
 *
 * <ul>
 *   <li>Timestamp
 *   <li>HTTP status
 *   <li>Error type
 *   <li>User-friendly message
 *   <li>Request path
 *   <li>Correlation ID (for tracing)
 * </ul>
 *
 * <p>Correlation ID is extracted from Reactor Context and included in both logs and error responses
 * (NFR-038 to NFR-043).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String CORRELATION_ID_KEY = "correlationId";

  /**
   * Handle domain exceptions (business logic errors).
   *
   * @param ex domain exception
   * @param exchange server web exchange
   * @return Mono with error response
   */
  @ExceptionHandler(DomainException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleDomainException(
      DomainException ex, ServerWebExchange exchange) {
    return Mono.deferContextual(
        ctx -> {
          String correlationId = ctx.getOrDefault(CORRELATION_ID_KEY, "N/A").toString();
          MDC.put(CORRELATION_ID_KEY, correlationId);

          log.warn(
              "Domain exception [correlationId={}]: {} - {}",
              correlationId,
              ex.getClass().getSimpleName(),
              ex.getMessage());

          ErrorResponse errorResponse =
              ErrorResponse.builder()
                  .timestamp(LocalDateTime.now())
                  .status(ex.getHttpStatus().value())
                  .error(ex.getErrorCode())
                  .message(ex.getMessage())
                  .path(exchange.getRequest().getPath().value())
                  .correlationId(correlationId)
                  .build();

          MDC.remove(CORRELATION_ID_KEY);
          return Mono.just(ResponseEntity.status(ex.getHttpStatus()).body(errorResponse));
        });
  }

  /**
   * Handle validation exceptions (Bean Validation).
   *
   * @param ex validation exception
   * @param exchange server web exchange
   * @return Mono with error response
   */
  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleValidationException(
      WebExchangeBindException ex, ServerWebExchange exchange) {
    return Mono.deferContextual(
        ctx -> {
          String correlationId = ctx.getOrDefault(CORRELATION_ID_KEY, "N/A").toString();
          MDC.put(CORRELATION_ID_KEY, correlationId);

          String errorMessage =
              ex.getBindingResult().getFieldErrors().stream()
                  .map(error -> error.getField() + ": " + error.getDefaultMessage())
                  .reduce((msg1, msg2) -> msg1 + ", " + msg2)
                  .orElse("Validation failed");

          log.warn("Validation error [correlationId={}]: {}", correlationId, errorMessage);

          ErrorResponse errorResponse =
              ErrorResponse.builder()
                  .timestamp(LocalDateTime.now())
                  .status(HttpStatus.BAD_REQUEST.value())
                  .error("Validation Error")
                  .message(errorMessage)
                  .path(exchange.getRequest().getPath().value())
                  .correlationId(correlationId)
                  .build();

          MDC.remove(CORRELATION_ID_KEY);
          return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        });
  }

  @ExceptionHandler(RuntimeException.class)
  public Mono<ResponseEntity<ErrorResponse>> handleRuntimeException(
      RuntimeException ex, ServerWebExchange exchange) {
    return Mono.deferContextual(
        ctx -> {
          String correlationId = ctx.getOrDefault(CORRELATION_ID_KEY, "N/A").toString();
          MDC.put(CORRELATION_ID_KEY, correlationId);

          log.error("Runtime exception [correlationId={}]: {}", correlationId, ex.getMessage(), ex);

          ErrorResponse errorResponse =
              ErrorResponse.builder()
                  .timestamp(LocalDateTime.now())
                  .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                  .error("Internal Server Error")
                  .message(ex.getMessage())
                  .path(exchange.getRequest().getPath().value())
                  .correlationId(correlationId)
                  .build();

          MDC.remove(CORRELATION_ID_KEY);
          return Mono.just(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        });
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ErrorResponse>> handleException(
      Exception ex, ServerWebExchange exchange) {
    return Mono.deferContextual(
        ctx -> {
          String correlationId = ctx.getOrDefault(CORRELATION_ID_KEY, "N/A").toString();
          MDC.put(CORRELATION_ID_KEY, correlationId);

          log.error(
              "Unexpected exception [correlationId={}]: {}", correlationId, ex.getMessage(), ex);

          ErrorResponse errorResponse =
              ErrorResponse.builder()
                  .timestamp(LocalDateTime.now())
                  .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                  .error("Internal Server Error")
                  .message("An unexpected error occurred")
                  .path(exchange.getRequest().getPath().value())
                  .correlationId(correlationId)
                  .build();

          MDC.remove(CORRELATION_ID_KEY);
          return Mono.just(
              ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        });
  }
}
