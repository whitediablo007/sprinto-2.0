package ru.get.tms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.get.tms.api.websocket.NotificationWebSocketHandler;

/**
 * Integration tests for WebSocket functionality.
 *
 * <p>Tests WebSocket connection, message broadcasting, and reactive stream behavior. Uses
 * Testcontainers for database and WebSocketClient for testing WebSocket endpoints.
 *
 * @see ru.get.tms.config.WebSocketConfig
 * @see ru.get.tms.api.websocket.NotificationWebSocketHandler
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@DisplayName("WebSocket Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("resource")
class WebSocketTest {

  @Container
  private static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:15-alpine")
          .withDatabaseName("testdb")
          .withUsername("test")
          .withPassword("test");

  @LocalServerPort private int port;

  @Autowired private NotificationWebSocketHandler notificationHandler;

  private WebSocketClient webSocketClient;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.r2dbc.url", () -> postgres.getJdbcUrl().replace("jdbc:", "r2dbc:"));
    registry.add("spring.r2dbc.username", postgres::getUsername);
    registry.add("spring.r2dbc.password", postgres::getPassword);
    registry.add(
        "spring.flyway.url",
        () ->
            String.format(
                "jdbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
    registry.add("spring.flyway.user", postgres::getUsername);
    registry.add("spring.flyway.password", postgres::getPassword);
  }

  @BeforeEach
  void setUp() {
    webSocketClient = new ReactorNettyWebSocketClient();
  }

  // ========== CONNECTION TESTS ==========

  @Test
  @Order(1)
  @DisplayName("WebSocket - should successfully connect to /ws/notifications endpoint")
  void webSocket_ConnectSuccess() {
    // Arrange
    URI uri = URI.create("ws://localhost:" + port + "/ws/notifications");
    AtomicReference<Boolean> connected = new AtomicReference<>(false);

    // Act
    Mono<Void> execution =
        webSocketClient.execute(
            uri,
            session -> {
              connected.set(true);
              return Mono.delay(Duration.ofMillis(100)).then();
            });

    // Assert
    StepVerifier.create(execution).expectComplete().verify(Duration.ofSeconds(5));

    assertThat(connected.get()).isTrue();
  }

  @Test
  @Order(2)
  @DisplayName("WebSocket - should receive broadcasted message")
  void webSocket_ReceiveBroadcastMessage() {
    // Arrange
    URI uri = URI.create("ws://localhost:" + port + "/ws/notifications");
    String testMessage = "Test notification message";
    AtomicReference<String> receivedMessage = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);

    // Act - Start WebSocket client in a separate thread
    webSocketClient
        .execute(
            uri,
            session ->
                session
                    .receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(
                        msg -> {
                          receivedMessage.set(msg);
                          latch.countDown();
                        })
                    .then())
        .subscribe();

    // Wait a bit for connection to establish
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Broadcast message
    notificationHandler.broadcast(testMessage);

    // Wait for message to arrive
    try {
      boolean received = latch.await(3, TimeUnit.SECONDS);
      assertThat(received).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("Test was interrupted");
    }

    // Assert
    assertThat(receivedMessage.get()).isEqualTo(testMessage);
  }

  @Test
  @Order(3)
  @DisplayName("WebSocket - should receive multiple messages in order")
  void webSocket_ReceiveMultipleMessages() {
    // Arrange
    URI uri = URI.create("ws://localhost:" + port + "/ws/notifications");
    String message1 = "First message";
    String message2 = "Second message";
    String message3 = "Third message";
    List<String> receivedMessages = new ArrayList<>();
    CountDownLatch latch = new CountDownLatch(3);

    // Act - Start WebSocket client
    webSocketClient
        .execute(
            uri,
            session ->
                session
                    .receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .take(3)
                    .doOnNext(
                        msg -> {
                          receivedMessages.add(msg);
                          latch.countDown();
                        })
                    .then())
        .subscribe();

    // Wait for connection
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Broadcast messages
    notificationHandler.broadcast(message1);
    notificationHandler.broadcast(message2);
    notificationHandler.broadcast(message3);

    // Wait for all messages to arrive
    try {
      boolean received = latch.await(5, TimeUnit.SECONDS);
      assertThat(received).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("Test was interrupted");
    }

    // Assert
    assertThat(receivedMessages).containsExactly(message1, message2, message3);
  }

  // ========== MULTIPLE CLIENTS TESTS ==========

  @Test
  @Order(4)
  @DisplayName("WebSocket - should broadcast message to multiple clients")
  void webSocket_BroadcastToMultipleClients() {
    // Arrange
    URI uri = URI.create("ws://localhost:" + port + "/ws/notifications");
    String testMessage = "Broadcast to all clients";
    AtomicReference<String> receivedByClient1 = new AtomicReference<>();
    AtomicReference<String> receivedByClient2 = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(2);

    // Act - Create two WebSocket clients
    webSocketClient
        .execute(
            uri,
            session ->
                session
                    .receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(
                        msg -> {
                          receivedByClient1.set(msg);
                          latch.countDown();
                        })
                    .then())
        .subscribe();

    webSocketClient
        .execute(
            uri,
            session ->
                session
                    .receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .doOnNext(
                        msg -> {
                          receivedByClient2.set(msg);
                          latch.countDown();
                        })
                    .then())
        .subscribe();

    // Wait for connections
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Broadcast message
    notificationHandler.broadcast(testMessage);

    // Wait for both clients to receive the message
    try {
      boolean received = latch.await(5, TimeUnit.SECONDS);
      assertThat(received).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("Test was interrupted");
    }

    // Assert
    assertThat(receivedByClient1.get()).isEqualTo(testMessage);
    assertThat(receivedByClient2.get()).isEqualTo(testMessage);
  }

  // ========== ERROR HANDLING TESTS ==========

  @Test
  @Order(5)
  @DisplayName("WebSocket - should handle connection to non-existent endpoint")
  void webSocket_NonExistentEndpoint_HandlesError() {
    // Arrange
    URI uri = URI.create("ws://localhost:" + port + "/ws/non-existent");

    // Act
    Mono<Void> execution =
        webSocketClient.execute(uri, session -> Mono.delay(Duration.ofMillis(100)).then());

    // Assert - Should complete with error (404 or similar)
    StepVerifier.create(execution).expectError().verify(Duration.ofSeconds(5));
  }

  // ========== REACTIVE BACKPRESSURE TESTS ==========

  @Test
  @Order(6)
  @DisplayName("WebSocket - should handle backpressure with multiple rapid messages")
  void webSocket_HandleBackpressure() {
    // Arrange
    URI uri = URI.create("ws://localhost:" + port + "/ws/notifications");
    int messageCount = 100;
    AtomicInteger receivedCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(messageCount);

    // Act - Start WebSocket client
    webSocketClient
        .execute(
            uri,
            session ->
                session
                    .receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .take(messageCount)
                    .doOnNext(
                        msg -> {
                          receivedCount.incrementAndGet();
                          latch.countDown();
                        })
                    .then())
        .subscribe();

    // Wait for connection
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Send many messages rapidly
    for (int i = 0; i < messageCount; i++) {
      notificationHandler.broadcast("Message " + i);
    }

    // Wait for all messages to arrive
    try {
      boolean received = latch.await(10, TimeUnit.SECONDS);
      assertThat(received).isTrue();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      fail("Test was interrupted");
    }

    // Assert
    assertThat(receivedCount.get()).isEqualTo(messageCount);
  }

  // ========== CLEANUP TESTS ==========

  @Test
  @Order(7)
  @DisplayName("WebSocket - should properly close connection")
  void webSocket_CloseConnection() {
    // Arrange
    URI uri = URI.create("ws://localhost:" + port + "/ws/notifications");
    AtomicReference<Boolean> sessionClosed = new AtomicReference<>(false);

    // Act
    Mono<Void> execution =
        webSocketClient.execute(
            uri,
            session -> {
              return Mono.delay(Duration.ofMillis(100))
                  .then(session.close())
                  .doOnSuccess(v -> sessionClosed.set(true));
            });

    // Assert
    StepVerifier.create(execution).expectComplete().verify(Duration.ofSeconds(5));

    assertThat(sessionClosed.get()).isTrue();
  }
}
