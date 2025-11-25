import { Injectable, OnDestroy } from '@angular/core';
import { RxStomp, RxStompConfig } from '@stomp/rx-stomp';
import { BehaviorSubject, Observable, timer } from 'rxjs';
import { filter, map, retry, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/**
 * Connection state enum for WebSocket service.
 */
export enum ConnectionState {
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
  RECONNECTING = 'RECONNECTING',
  ERROR = 'ERROR'
}

/**
 * WebSocket Service with RxStomp for real-time communication.
 * 
 * Features:
 * - Auto-reconnect with exponential backoff (T629)
 * - Automatic resubscription to channels after reconnect (T630)
 * - Connection state management
 * - Support for user-specific queues and topic subscriptions
 * 
 * @see NFR-044 to NFR-049 for WebSocket requirements
 */
@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private rxStomp: RxStomp;
  private connectionState$ = new BehaviorSubject<ConnectionState>(ConnectionState.DISCONNECTED);
  private reconnectAttempts = 0;
  private readonly MAX_RECONNECT_ATTEMPTS = 10;
  private readonly BASE_RECONNECT_DELAY = 1000; // 1 second
  private readonly MAX_RECONNECT_DELAY = 30000; // 30 seconds
  private activeSubscriptions: Map<string, any> = new Map();

  constructor() {
    // Lazy initialization to avoid errors during app bootstrap
  }

  /**
   * Initialize RxStomp if not already initialized.
   */
  private ensureInitialized(): void {
    if (!this.rxStomp) {
      this.rxStomp = new RxStomp();
      this.configureRxStomp();
    }
  }

  /**
   * Configure RxStomp with connection settings and event handlers.
   */
  private configureRxStomp(): void {
    const stompConfig: RxStompConfig = {
      // WebSocket endpoint
      brokerURL: environment.wsUrl,

      // Auto-reconnect with exponential backoff (T629)
      reconnectDelay: this.BASE_RECONNECT_DELAY, // Base delay: 1 second
      maxReconnectDelay: this.MAX_RECONNECT_DELAY, // Max delay: 30 seconds
      // Note: RxStomp uses exponential backoff by default

      // Heartbeat configuration
      heartbeatIncoming: 10000, // Expect heartbeat every 10 seconds
      heartbeatOutgoing: 10000, // Send heartbeat every 10 seconds

      // Connection timeout
      connectionTimeout: 5000,

      // Debug logging (only in development)
      debug: (msg: string) => {
        if (!environment.production) {
          console.log('[WebSocket]', msg);
        }
      }
    };

    if (!this.rxStomp) {
      return;
    }

    this.rxStomp.configure(stompConfig);

    // Monitor connection state - with safety checks and delay
    setTimeout(() => {
      if (!this.rxStomp) {
        return;
      }

      try {
        if (this.rxStomp.connected$) {
          this.rxStomp.connected$.pipe(
            tap(() => {
              this.connectionState$.next(ConnectionState.CONNECTED);
              this.reconnectAttempts = 0;
              this.resubscribeToChannels(); // T630: Resubscribe after reconnect
            })
          ).subscribe();
        }

        if (this.rxStomp.connectionState$) {
          this.rxStomp.connectionState$.pipe(
            tap(state => {
              if (state === 0) { // CLOSED
                this.connectionState$.next(ConnectionState.DISCONNECTED);
              } else if (state === 1) { // TRYING
                this.connectionState$.next(
                  this.reconnectAttempts > 0 ? ConnectionState.RECONNECTING : ConnectionState.CONNECTING
                );
              }
            })
          ).subscribe();
        }
      } catch (error) {
        console.warn('[WebSocket] Error setting up connection monitoring:', error);
      }
    }, 0);
  }

  /**
   * Calculate reconnect delay with exponential backoff (T629).
   * 
   * Delay increases exponentially: 1s, 2s, 4s, 8s, 16s, 30s (max)
   * 
   * @returns delay in milliseconds
   */
  private calculateReconnectDelay(): number {
    if (this.reconnectAttempts >= this.MAX_RECONNECT_ATTEMPTS) {
      console.error('[WebSocket] Max reconnection attempts reached');
      this.connectionState$.next(ConnectionState.ERROR);
      return 0; // Stop reconnecting
    }

    this.reconnectAttempts++;
    const delay = Math.min(
      this.BASE_RECONNECT_DELAY * Math.pow(2, this.reconnectAttempts - 1),
      this.MAX_RECONNECT_DELAY
    );

    console.log(`[WebSocket] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.MAX_RECONNECT_ATTEMPTS})`);
    return delay;
  }

  /**
   * Resubscribe to all active channels after reconnection (T630).
   * 
   * Key channels include:
   * - /user/queue/timer
   * - /user/queue/notifications
   * - /topic/project/{projectId}
   * - /topic/task/{taskId}
   */
  private resubscribeToChannels(): void {
    console.log('[WebSocket] Resubscribing to channels after reconnect');
    
    // Create a copy of subscriptions to avoid modification during iteration
    const subscriptionsToRestore = new Map(this.activeSubscriptions);
    
    subscriptionsToRestore.forEach((callback, destination) => {
      console.log(`[WebSocket] Resubscribing to ${destination}`);
      this.subscribe(destination, callback);
    });
  }

  /**
   * Activate WebSocket connection.
   */
  public connect(): void {
    this.ensureInitialized();
    if (!this.rxStomp!.active) {
      console.log('[WebSocket] Activating connection');
      this.connectionState$.next(ConnectionState.CONNECTING);
      this.rxStomp!.activate();
    }
  }

  /**
   * Deactivate WebSocket connection.
   */
  public disconnect(): void {
    if (this.rxStomp.active) {
      console.log('[WebSocket] Deactivating connection');
      this.rxStomp.deactivate();
      this.activeSubscriptions.clear();
    }
  }

  /**
   * Subscribe to a destination (topic or queue).
   * 
   * @param destination - STOMP destination (e.g., '/topic/notifications')
   * @param callback - Callback function to handle incoming messages
   * @returns Observable of messages
   */
  public subscribe<T>(destination: string, callback?: (message: T) => void): Observable<T> {
    this.ensureInitialized();
    const observable = this.rxStomp!.watch(destination).pipe(
      map(message => JSON.parse(message.body) as T),
      tap(data => {
        if (callback) {
          callback(data);
        }
      })
    );

    // Store subscription for auto-resubscribe (T630)
    if (callback) {
      this.activeSubscriptions.set(destination, callback);
    }

    return observable;
  }

  /**
   * Unsubscribe from a destination.
   * 
   * @param destination - STOMP destination
   */
  public unsubscribe(destination: string): void {
    this.activeSubscriptions.delete(destination);
  }

  /**
   * Publish a message to a destination.
   * 
   * @param destination - STOMP destination (e.g., '/app/send-message')
   * @param body - Message body (will be JSON stringified)
   */
  public publish(destination: string, body: any): void {
    this.ensureInitialized();
    this.rxStomp!.publish({
      destination,
      body: JSON.stringify(body)
    });
  }

  /**
   * Get current connection state as Observable.
   */
  public getConnectionState(): Observable<ConnectionState> {
    return this.connectionState$.asObservable();
  }

  /**
   * Check if WebSocket is currently connected.
   */
  public isConnected(): boolean {
    return this.connectionState$.value === ConnectionState.CONNECTED;
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.connectionState$.complete();
  }
}

