package ru.get.tms.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.get.tms.exception.ConflictException;
import ru.get.tms.exception.DomainException;
import ru.get.tms.exception.ResourceNotFoundException;
import ru.get.tms.util.ReactiveErrorHandler;

/**
 * Unit tests for ReactiveErrorHandler (NFR-027).
 *
 * <p>Tests the reactive error handling patterns to ensure:
 *
 * <ul>
 *   <li>Domain exceptions are preserved in reactive streams
 *   <li>Generic exceptions are wrapped appropriately
 *   <li>Error handlers don't break reactive pipelines
 * </ul>
 */
@DisplayName("ReactiveErrorHandler Unit Tests")
class ReactiveErrorHandlerTest {

  @Test
  @DisplayName("handleNotFound should create ResourceNotFoundException in Mono")
  void shouldHandleNotFoundInMono() {
    // Given
    String resourceType = "User";
    UUID resourceId = UUID.randomUUID();

    // When
    Mono<String> result = ReactiveErrorHandler.handleNotFound(resourceType, resourceId);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof ResourceNotFoundException
                    && throwable.getMessage().contains(resourceType)
                    && throwable.getMessage().contains(resourceId.toString()))
        .verify();
  }

  @Test
  @DisplayName("handleFluxNotFound should create ResourceNotFoundException in Flux")
  void shouldHandleNotFoundInFlux() {
    // Given
    String resourceType = "Task";
    UUID resourceId = UUID.randomUUID();

    // When
    Flux<String> result = ReactiveErrorHandler.handleFluxNotFound(resourceType, resourceId);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(throwable -> throwable instanceof ResourceNotFoundException)
        .verify();
  }

  @Test
  @DisplayName("handleError should preserve domain exceptions")
  void shouldPreserveDomainExceptions() {
    // Given
    DomainException domainException = new ConflictException("Email already exists");

    // When
    Mono<String> result = ReactiveErrorHandler.handleError(domainException);

    // Then
    StepVerifier.create(result).expectError(ConflictException.class).verify();
  }

  @Test
  @DisplayName("handleError should wrap non-domain exceptions")
  void shouldWrapNonDomainExceptions() {
    // Given
    Exception genericException = new IllegalArgumentException("Invalid argument");

    // When
    Mono<String> result = ReactiveErrorHandler.handleError(genericException);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().contains("unexpected error")
                    && throwable.getCause() == genericException)
        .verify();
  }

  @Test
  @DisplayName("handleFluxError should preserve domain exceptions in Flux")
  void shouldPreserveDomainExceptionsInFlux() {
    // Given
    DomainException domainException = new ResourceNotFoundException("Project not found");

    // When
    Flux<String> result = ReactiveErrorHandler.handleFluxError(domainException);

    // Then
    StepVerifier.create(result).expectError(ResourceNotFoundException.class).verify();
  }

  @Test
  @DisplayName("handleEmptyResult should create ResourceNotFoundException with message")
  void shouldHandleEmptyResult() {
    // Given
    String errorMessage = "No active timer found";

    // When
    Mono<String> result = ReactiveErrorHandler.handleEmptyResult(errorMessage);

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof ResourceNotFoundException
                    && throwable.getMessage().equals(errorMessage))
        .verify();
  }

  @Test
  @DisplayName("mapDatabaseError should map data integrity violations to ConflictException")
  void shouldMapDataIntegrityViolation() {
    // Given
    Exception dbException =
        new RuntimeException("org.springframework.dao.DataIntegrityViolationException");

    // When
    Throwable mapped = ReactiveErrorHandler.mapDatabaseError().apply(dbException);

    // Then
    assertThat(mapped).isInstanceOf(ConflictException.class);
  }

  @Test
  @DisplayName("mapDatabaseError should map empty results to ResourceNotFoundException")
  void shouldMapEmptyResultException() {
    // Given
    Exception dbException =
        new RuntimeException("org.springframework.dao.EmptyResultDataAccessException");

    // When
    Throwable mapped = ReactiveErrorHandler.mapDatabaseError().apply(dbException);

    // Then
    assertThat(mapped).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  @DisplayName("mapDatabaseError should preserve unmapped exceptions")
  void shouldPreserveUnmappedExceptions() {
    // Given
    Exception genericException = new IllegalStateException("Some other error");

    // When
    Throwable mapped = ReactiveErrorHandler.mapDatabaseError().apply(genericException);

    // Then
    assertThat(mapped).isSameAs(genericException);
  }

  @Test
  @DisplayName("chain should apply multiple error handlers in sequence")
  void shouldChainMultipleErrorHandlers() {
    // Given
    Exception originalException = new IllegalArgumentException("Test");

    // When
    Mono<String> result =
        Mono.<String>error(originalException)
            .onErrorResume(
                ReactiveErrorHandler.chain(
                    e ->
                        e instanceof IllegalArgumentException
                            ? Mono.just("fallback")
                            : Mono.error(e),
                    ReactiveErrorHandler::handleError));

    // Then
    StepVerifier.create(result).expectNext("fallback").verifyComplete();
  }

  @Test
  @DisplayName("Reactive pipeline should not break on error with onErrorResume")
  void shouldNotBreakReactivePipeline() {
    // Given
    Flux<Integer> numbers =
        Flux.just(1, 2, 3, 4, 5)
            .flatMap(
                n -> {
                  if (n == 3) {
                    return Mono.error(new IllegalArgumentException("Error at 3"));
                  }
                  return Mono.just(n);
                })
            .onErrorResume(
                e -> {
                  if (e instanceof IllegalArgumentException) {
                    return Mono.just(999); // Fallback value
                  }
                  return Mono.error(e);
                });

    // When & Then
    StepVerifier.create(numbers).expectNext(1, 2, 999, 4, 5).verifyComplete();
  }

  @Test
  @DisplayName("switchIfEmpty with handleNotFound should work in reactive pipeline")
  void shouldWorkWithSwitchIfEmpty() {
    // Given
    UUID userId = UUID.randomUUID();
    Mono<String> emptyMono = Mono.empty();

    // When
    Mono<String> result =
        emptyMono.switchIfEmpty(ReactiveErrorHandler.handleNotFound("User", userId));

    // Then
    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof ResourceNotFoundException
                    && throwable.getMessage().contains("User"))
        .verify();
  }
}
