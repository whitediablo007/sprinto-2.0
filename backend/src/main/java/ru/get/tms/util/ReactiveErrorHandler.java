package ru.get.tms.util;

import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.exception.DomainException;

/**
 * Utility class for consistent reactive error handling (NFR-027).
 *
 * <p>Provides standardized error handling patterns for WebFlux services using onErrorResume and
 * onErrorMap operators.
 *
 * <p><b>Usage in services:</b>
 *
 * <pre>{@code
 * public Mono<User> findUserById(UUID userId) {
 *   return userRepository.findById(userId)
 *       .switchIfEmpty(ReactiveErrorHandler.handleNotFound("User", userId))
 *       .onErrorResume(ReactiveErrorHandler::handleError);
 * }
 * }</pre>
 *
 * <p><b>Key principles (Constitution Principle I):</b>
 *
 * <ul>
 *   <li>Never blocks reactive pipeline
 *   <li>Preserves error signals in reactive streams
 *   <li>Maps domain exceptions to appropriate HTTP status codes
 *   <li>Logs errors with context for debugging
 * </ul>
 *
 * @see DomainException
 */
@Slf4j
public final class ReactiveErrorHandler {

  private ReactiveErrorHandler() {
    // Utility class
  }

  /**
   * Handle Mono not found scenario with domain exception.
   *
   * @param resourceType type of resource (e.g., "User", "Project")
   * @param resourceId resource identifier
   * @param <T> return type
   * @return Mono error with ResourceNotFoundException
   */
  public static <T> Mono<T> handleNotFound(String resourceType, Object resourceId) {
    return Mono.error(new ru.get.tms.exception.ResourceNotFoundException(resourceType, resourceId));
  }

  /**
   * Handle Flux not found scenario with domain exception.
   *
   * @param resourceType type of resource
   * @param resourceId resource identifier
   * @param <T> return type
   * @return Flux error with ResourceNotFoundException
   */
  public static <T> Flux<T> handleFluxNotFound(String resourceType, Object resourceId) {
    return Flux.error(new ru.get.tms.exception.ResourceNotFoundException(resourceType, resourceId));
  }

  /**
   * Generic error handler for reactive pipelines.
   *
   * <p>Wraps non-domain exceptions in RuntimeException and logs them.
   *
   * @param throwable the error
   * @param <T> return type
   * @return Mono error with appropriate exception
   */
  public static <T> Mono<T> handleError(Throwable throwable) {
    if (throwable instanceof DomainException) {
      // Domain exceptions are already properly formatted
      log.debug("Domain exception in reactive pipeline: {}", throwable.getMessage());
      return Mono.error(throwable);
    }

    // Wrap unexpected exceptions
    log.error("Unexpected error in reactive pipeline", throwable);
    return Mono.error(
        new RuntimeException("An unexpected error occurred: " + throwable.getMessage(), throwable));
  }

  /**
   * Error handler for Flux.
   *
   * @param throwable the error
   * @param <T> return type
   * @return Flux error with appropriate exception
   */
  public static <T> Flux<T> handleFluxError(Throwable throwable) {
    if (throwable instanceof DomainException) {
      log.debug("Domain exception in reactive pipeline: {}", throwable.getMessage());
      return Flux.error(throwable);
    }

    log.error("Unexpected error in reactive pipeline", throwable);
    return Flux.error(
        new RuntimeException("An unexpected error occurred: " + throwable.getMessage(), throwable));
  }

  /**
   * Map database errors to domain exceptions.
   *
   * <p>Common patterns:
   *
   * <ul>
   *   <li>R2dbcDataIntegrityViolationException → ConflictException
   *   <li>EmptyResultDataAccessException → ResourceNotFoundException
   * </ul>
   *
   * @return Function for onErrorMap operator
   */
  public static Function<Throwable, Throwable> mapDatabaseError() {
    return throwable -> {
      if (throwable.getClass().getName().contains("DataIntegrityViolationException")) {
        log.warn("Data integrity violation: {}", throwable.getMessage());
        return new ru.get.tms.exception.ConflictException(
            "Operation conflicts with existing data", throwable);
      }

      if (throwable.getClass().getName().contains("EmptyResultDataAccessException")) {
        log.debug("Empty result from database: {}", throwable.getMessage());
        return new ru.get.tms.exception.ResourceNotFoundException("Resource not found");
      }

      // Return original exception if no mapping needed
      return throwable;
    };
  }

  /**
   * Handle switchIfEmpty scenario with custom error.
   *
   * @param errorMessage error message
   * @param <T> return type
   * @return Mono error with RuntimeException
   */
  public static <T> Mono<T> handleEmptyResult(String errorMessage) {
    return Mono.error(new ru.get.tms.exception.ResourceNotFoundException(errorMessage));
  }

  /**
   * Handle Flux switchIfEmpty scenario with custom error.
   *
   * @param errorMessage error message
   * @param <T> return type
   * @return Flux error with RuntimeException
   */
  public static <T> Flux<T> handleFluxEmptyResult(String errorMessage) {
    return Flux.error(new ru.get.tms.exception.ResourceNotFoundException(errorMessage));
  }

  /**
   * Log and re-throw error (useful for debugging).
   *
   * @param context context description
   * @return Function for doOnError operator
   */
  public static Function<Throwable, Mono<Void>> logError(String context) {
    return throwable -> {
      log.error("Error in {}: {}", context, throwable.getMessage(), throwable);
      return Mono.empty();
    };
  }

  /**
   * Chain multiple error handlers.
   *
   * <p>Example:
   *
   * <pre>{@code
   * .onErrorResume(ReactiveErrorHandler.chain(
   *     e -> e instanceof TimeoutException ? Mono.just(fallbackValue) : Mono.error(e),
   *     ReactiveErrorHandler::handleError
   * ))
   * }</pre>
   *
   * @param handlers array of error handlers
   * @param <T> return type
   * @return Combined error handler
   */
  @SafeVarargs
  public static <T> Function<Throwable, Mono<T>> chain(Function<Throwable, Mono<T>>... handlers) {
    return throwable -> {
      Mono<T> result = Mono.error(throwable);
      for (Function<Throwable, Mono<T>> handler : handlers) {
        result = result.onErrorResume(handler);
      }
      return result;
    };
  }
}
