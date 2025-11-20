package ru.get.tms.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when business logic validation fails.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>Attempting to start second timer while one is active
 *   <li>Circular task dependency detected
 *   <li>Cannot delete project with active tasks
 *   <li>Maximum hierarchy level exceeded
 * </ul>
 *
 * <p>Maps to HTTP 422 UNPROCESSABLE ENTITY.
 */
public class BusinessLogicException extends DomainException {

  private static final String ERROR_CODE = "BUSINESS_LOGIC_ERROR";

  public BusinessLogicException(String message) {
    super(message, HttpStatus.UNPROCESSABLE_ENTITY, ERROR_CODE);
  }

  public BusinessLogicException(String message, Throwable cause) {
    super(message, HttpStatus.UNPROCESSABLE_ENTITY, ERROR_CODE, cause);
  }

  public BusinessLogicException(String message, String customErrorCode) {
    super(message, HttpStatus.UNPROCESSABLE_ENTITY, customErrorCode);
  }
}
