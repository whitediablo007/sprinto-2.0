package ru.get.tms.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested resource is not found.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>User not found by ID
 *   <li>Project not found
 *   <li>Task not found
 * </ul>
 *
 * <p>Maps to HTTP 404 NOT FOUND.
 */
public class ResourceNotFoundException extends DomainException {

  private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

  public ResourceNotFoundException(String resourceType, Object resourceId) {
    super(
        String.format("%s with id '%s' not found", resourceType, resourceId),
        HttpStatus.NOT_FOUND,
        ERROR_CODE);
  }

  public ResourceNotFoundException(String message) {
    super(message, HttpStatus.NOT_FOUND, ERROR_CODE);
  }

  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, HttpStatus.NOT_FOUND, ERROR_CODE, cause);
  }
}
