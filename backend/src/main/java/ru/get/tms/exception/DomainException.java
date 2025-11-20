package ru.get.tms.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base domain exception for all business logic exceptions (NFR-027).
 *
 * <p>All domain-specific exceptions should extend this class to ensure consistent error handling in
 * reactive WebFlux services.
 *
 * <p>Provides:
 *
 * <ul>
 *   <li>HTTP status code mapping
 *   <li>Error code for client-side error handling
 *   <li>User-friendly error message
 *   <li>Optional cause for debugging
 * </ul>
 *
 * <p>Usage in reactive services: Use with {@link ReactiveErrorHandler} for consistent error
 * mapping.
 */
@Getter
public abstract class DomainException extends RuntimeException {

  private final HttpStatus httpStatus;
  private final String errorCode;

  protected DomainException(String message, HttpStatus httpStatus, String errorCode) {
    super(message);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }

  protected DomainException(
      String message, HttpStatus httpStatus, String errorCode, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
    this.errorCode = errorCode;
  }
}
