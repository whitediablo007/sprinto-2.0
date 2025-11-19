package ru.get.tms.api.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Reactive WebSocket handler for real-time notifications. Handles WebSocket connections and
 * broadcasts notifications to connected clients.
 */
@Component
public class NotificationWebSocketHandler implements WebSocketHandler {

  private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

  /**
   * Handles incoming WebSocket connection. Sends notification messages to the client as they
   * arrive.
   *
   * @param session WebSocket session
   * @return Mono<Void> completion signal
   */
  @Override
  public Mono<Void> handle(WebSocketSession session) {
    // Send notifications to client as they arrive
    return session.send(sink.asFlux().map(session::textMessage));
  }

  /**
   * Broadcasts a notification to all connected WebSocket clients.
   *
   * @param message notification message to broadcast
   */
  public void broadcast(String message) {
    sink.tryEmitNext(message);
  }
}
