package ru.get.tms.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for Structured Logging with Correlation ID (NFR-038 to NFR-043).
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Correlation ID is extracted from request headers
 *   <li>Correlation ID is generated if missing
 *   <li>Correlation ID is returned in response headers
 *   <li>Correlation ID is included in error responses
 *   <li>Correlation ID propagates through reactive chain
 * </ul>
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.profiles.active=test"})
@AutoConfigureWebTestClient
public class StructuredLoggingTest {

  @Autowired private WebTestClient webTestClient;

  private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
  private static final String TEST_CORRELATION_ID = "test-correlation-123";

  @Test
  void shouldReturnCorrelationIdInResponseWhenProvidedInRequest() {
    webTestClient
        .get()
        .uri("/api/auth/test")
        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
        .exchange()
        .expectHeader()
        .valueEquals(CORRELATION_ID_HEADER, TEST_CORRELATION_ID);
  }

  @Test
  void shouldGenerateCorrelationIdWhenNotProvidedInRequest() {
    webTestClient
        .get()
        .uri("/api/auth/test")
        .exchange()
        .expectHeader()
        .exists(CORRELATION_ID_HEADER)
        .expectHeader()
        .value(
            CORRELATION_ID_HEADER,
            correlationId -> {
              assertThat(correlationId).isNotBlank();
              assertThat(correlationId).hasSize(36); // UUID format
            });
  }

  @Test
  void shouldIncludeCorrelationIdInErrorResponse() {
    webTestClient
        .post()
        .uri("/api/auth/register")
        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
        .bodyValue(
            """
                    {
                        "email": "invalid-email",
                        "password": "short",
                        "name": "Test User"
                    }
                    """)
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .exchange()
        .expectStatus()
        .isBadRequest()
        .expectHeader()
        .valueEquals(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
        .expectBody()
        .jsonPath("$.correlationId")
        .isEqualTo(TEST_CORRELATION_ID)
        .jsonPath("$.error")
        .exists()
        .jsonPath("$.message")
        .exists();
  }

  @Test
  void shouldPreserveCorrelationIdThroughMultipleRequests() {
    String correlationId1 = "request-1-correlation-id";
    String correlationId2 = "request-2-correlation-id";

    // First request
    webTestClient
        .get()
        .uri("/api/auth/test")
        .header(CORRELATION_ID_HEADER, correlationId1)
        .exchange()
        .expectHeader()
        .valueEquals(CORRELATION_ID_HEADER, correlationId1);

    // Second request with different correlation ID
    webTestClient
        .get()
        .uri("/api/auth/test")
        .header(CORRELATION_ID_HEADER, correlationId2)
        .exchange()
        .expectHeader()
        .valueEquals(CORRELATION_ID_HEADER, correlationId2);
  }

  @Test
  void shouldHandleEmptyCorrelationIdHeader() {
    webTestClient
        .get()
        .uri("/api/auth/test")
        .header(CORRELATION_ID_HEADER, "")
        .exchange()
        .expectHeader()
        .exists(CORRELATION_ID_HEADER)
        .expectHeader()
        .value(
            CORRELATION_ID_HEADER,
            correlationId -> {
              assertThat(correlationId).isNotBlank();
              assertThat(correlationId).hasSize(36); // UUID format
            });
  }

  @Test
  void shouldIncludeCorrelationIdInSuccessResponse() {
    webTestClient
        .post()
        .uri("/api/auth/login")
        .header(CORRELATION_ID_HEADER, TEST_CORRELATION_ID)
        .bodyValue(
            """
                    {
                        "email": "test@example.com",
                        "password": "ValidPassword123!"
                    }
                    """)
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .exchange()
        .expectHeader()
        .valueEquals(CORRELATION_ID_HEADER, TEST_CORRELATION_ID);
  }
}
