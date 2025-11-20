package ru.get.tms.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when authentication fails or is missing.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Invalid credentials
 *   <li>Missing JWT token
 *   <li>Expired token
 * </ul>
 *
 * <p>Maps to HTTP 401 UNAUTHORIZED.
 */
public class UnauthorizedException extends DomainException {

  private static final String ERROR_CODE = "UNAUTHORIZED";

  public UnauthorizedException(String message) {
    super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE);
  }

  public UnauthorizedException(String message, Throwable cause) {
    super(message, HttpStatus.UNAUTHORIZED, ERROR_CODE, cause);
  }
}
