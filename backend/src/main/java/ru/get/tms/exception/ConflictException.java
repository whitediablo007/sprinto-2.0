package ru.get.tms.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when resource conflict occurs.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Email already registered
 *   <li>Duplicate project name
 *   <li>Concurrent modification conflict
 * </ul>
 *
 * <p>Maps to HTTP 409 CONFLICT.
 */
public class ConflictException extends DomainException {

  private static final String ERROR_CODE = "CONFLICT";

  public ConflictException(String message) {
    super(message, HttpStatus.CONFLICT, ERROR_CODE);
  }

  public ConflictException(String message, Throwable cause) {
    super(message, HttpStatus.CONFLICT, ERROR_CODE, cause);
  }
}
