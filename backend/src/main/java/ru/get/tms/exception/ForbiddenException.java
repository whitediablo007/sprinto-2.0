package ru.get.tms.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when user lacks permissions for an operation.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>User trying to delete project they don't own
 *   <li>Observer trying to edit task
 *   <li>Non-admin trying to access admin functionality
 * </ul>
 *
 * <p>Maps to HTTP 403 FORBIDDEN.
 */
public class ForbiddenException extends DomainException {

  private static final String ERROR_CODE = "FORBIDDEN";

  public ForbiddenException(String message) {
    super(message, HttpStatus.FORBIDDEN, ERROR_CODE);
  }

  public ForbiddenException(String message, Throwable cause) {
    super(message, HttpStatus.FORBIDDEN, ERROR_CODE, cause);
  }
}
