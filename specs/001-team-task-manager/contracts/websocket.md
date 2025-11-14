# WebSocket Protocol

**Проект**: Система управления проектами и задачами для малых команд  
**Версия**: 1.0  
**Дата**: 2025-11-14

## Обзор

WebSocket используется для real-time коммуникации между сервером и клиентами. Основные use cases:
- Обновление активного таймера каждую секунду
- Доставка уведомлений в реальном времени
- Синхронизация изменений в проектах и задачах
- Оповещение о действиях других участников команды

**Протокол**: STOMP (Simple Text Oriented Messaging Protocol) over WebSocket  
**Library**: Spring WebFlux WebSocket + SockJS fallback для старых браузеров

---

## Подключение

### Endpoint

```
ws://localhost:8080/ws
wss://api.sprinto.com/ws  (production)
```

### Аутентификация

JWT токен передается в query parameter при подключении:

```javascript
const socket = new SockJS('http://localhost:8080/ws?token=<JWT_ACCESS_TOKEN>');
const stompClient = Stomp.over(socket);
```

**Валидация токена**:
- Сервер проверяет JWT при handshake
- Если токен невалиден или истек → соединение отклоняется (HTTP 401)
- Если токен истекает во время сессии → клиент получает сообщение `/user/queue/errors` и должен переподключиться с новым токеном

### Heartbeat

**Настройки**:
- Client → Server: каждые 30 секунд (ping)
- Server → Client: каждые 30 секунд (pong)

Если heartbeat не получен в течение 60 секунд → соединение разрывается.

**STOMP Configuration**:
```javascript
stompClient.connect(
  {}, // headers
  (frame) => {
    console.log('Connected:', frame);
  },
  (error) => {
    console.error('Connection error:', error);
  }
);

stompClient.heartbeat.outgoing = 30000; // 30 seconds
stompClient.heartbeat.incoming = 30000; // 30 seconds
```

---

## Темы (Topics) и Очереди (Queues)

### 1. Активный таймер пользователя

**Topic**: `/user/queue/timer`

**Назначение**: Обновление активного таймера каждую секунду

**Подписка** (Client → Server):
```javascript
stompClient.subscribe('/user/queue/timer', (message) => {
  const timerUpdate = JSON.parse(message.body);
  console.log('Timer update:', timerUpdate);
});
```

**Сообщения** (Server → Client):

```json
{
  "type": "TIMER_UPDATE",
  "data": {
    "entryId": "uuid",
    "taskId": "uuid",
    "taskTitle": "Implement authentication",
    "projectName": "Backend API",
    "projectColor": "#3B82F6",
    "startTime": "2025-11-14T10:00:00Z",
    "elapsedSeconds": 3605
  },
  "timestamp": "2025-11-14T11:00:05Z"
}
```

**Частота**: Каждую секунду, пока таймер активен

**Запуск таймера**:
- Клиент вызывает REST API `POST /api/time-entries/timer/start`
- Сервер создает запись в БД
- Сервер начинает отправлять обновления через WebSocket

**Остановка таймера**:
- Клиент вызывает REST API `POST /api/time-entries/timer/stop`
- Сервер останавливает таймер и прекращает отправку обновлений

---

### 2. Персональные уведомления

**Topic**: `/user/queue/notifications`

**Назначение**: Доставка персональных уведомлений пользователю

**Подписка** (Client → Server):
```javascript
stompClient.subscribe('/user/queue/notifications', (message) => {
  const notification = JSON.parse(message.body);
  displayNotification(notification);
});
```

**Сообщения** (Server → Client):

```json
{
  "type": "NOTIFICATION",
  "data": {
    "id": "uuid",
    "notificationType": "TASK_ASSIGNED",
    "title": "Новая задача назначена",
    "message": "Вам назначена задача 'Implement WebSocket protocol'",
    "relatedEntityType": "TASK",
    "relatedEntityId": "task-uuid",
    "createdAt": "2025-11-14T11:00:00Z"
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

**Типы уведомлений** (notificationType):
- `TASK_ASSIGNED` - назначена задача
- `STATUS_CHANGED` - изменен статус задачи
- `NEW_COMMENT` - новый комментарий
- `DEADLINE_APPROACHING` - приближается дедлайн (за 24 часа)
- `DEADLINE_EXPIRED` - просрочен дедлайн
- `MENTIONED` - упоминание в комментарии
- `ADDED_TO_PROJECT` - добавлен в проект
- `PROJECT_UPDATED` - обновлен проект

**Badge Update**:
Сервер также отправляет обновление счетчика непрочитанных:

```json
{
  "type": "UNREAD_COUNT",
  "data": {
    "count": 5
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

---

### 3. События проекта

**Topic**: `/topic/project.{projectId}`

**Назначение**: Синхронизация изменений в проекте между участниками

**Подписка** (Client → Server):
```javascript
const projectId = 'uuid';
stompClient.subscribe(`/topic/project.${projectId}`, (message) => {
  const event = JSON.parse(message.body);
  handleProjectEvent(event);
});
```

**Сообщения** (Server → Client):

#### 3.1. Новая задача создана

```json
{
  "type": "TASK_CREATED",
  "data": {
    "task": {
      "id": "uuid",
      "title": "New task",
      "status": "NEW",
      "priority": "HIGH",
      "assigneeId": "uuid",
      "createdBy": "uuid"
    }
  },
  "actor": {
    "id": "uuid",
    "name": "John Doe"
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 3.2. Задача обновлена

```json
{
  "type": "TASK_UPDATED",
  "data": {
    "taskId": "uuid",
    "changes": {
      "status": {
        "old": "IN_PROGRESS",
        "new": "COMPLETED"
      }
    }
  },
  "actor": {
    "id": "uuid",
    "name": "Jane Smith"
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 3.3. Задача удалена

```json
{
  "type": "TASK_DELETED",
  "data": {
    "taskId": "uuid"
  },
  "actor": {
    "id": "uuid",
    "name": "John Doe"
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 3.4. Новый участник добавлен

```json
{
  "type": "MEMBER_ADDED",
  "data": {
    "member": {
      "userId": "uuid",
      "name": "Alice Johnson",
      "role": "PROJECT_MEMBER"
    }
  },
  "actor": {
    "id": "uuid",
    "name": "Project Owner"
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 3.5. Участник удален

```json
{
  "type": "MEMBER_REMOVED",
  "data": {
    "userId": "uuid",
    "name": "Bob Williams"
  },
  "actor": {
    "id": "uuid",
    "name": "Project Owner"
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

---

### 4. События задачи

**Topic**: `/topic/task.{taskId}`

**Назначение**: Real-time обновления конкретной задачи (для детальной страницы задачи)

**Подписка** (Client → Server):
```javascript
const taskId = 'uuid';
stompClient.subscribe(`/topic/task.${taskId}`, (message) => {
  const event = JSON.parse(message.body);
  handleTaskEvent(event);
});
```

**Сообщения** (Server → Client):

#### 4.1. Новый комментарий

```json
{
  "type": "COMMENT_ADDED",
  "data": {
    "comment": {
      "id": "uuid",
      "userId": "uuid",
      "userName": "John Doe",
      "userAvatar": "url",
      "text": "This looks good!",
      "mentionedUsers": ["uuid1", "uuid2"],
      "createdAt": "2025-11-14T11:00:00Z"
    }
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 4.2. Комментарий обновлен

```json
{
  "type": "COMMENT_UPDATED",
  "data": {
    "commentId": "uuid",
    "text": "Updated comment text",
    "updatedAt": "2025-11-14T11:05:00Z"
  },
  "timestamp": "2025-11-14T11:05:00Z"
}
```

#### 4.3. Комментарий удален

```json
{
  "type": "COMMENT_DELETED",
  "data": {
    "commentId": "uuid"
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 4.4. Файл загружен

```json
{
  "type": "FILE_UPLOADED",
  "data": {
    "file": {
      "id": "uuid",
      "filename": "document.pdf",
      "fileSize": 102400,
      "mimeType": "application/pdf",
      "uploadedBy": "uuid",
      "uploadedByName": "Jane Smith",
      "uploadedAt": "2025-11-14T11:00:00Z",
      "downloadUrl": "/api/files/{fileId}"
    }
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 4.5. Статус изменен

```json
{
  "type": "STATUS_CHANGED",
  "data": {
    "oldStatus": "IN_PROGRESS",
    "newStatus": "COMPLETED",
    "changedBy": {
      "id": "uuid",
      "name": "John Doe"
    }
  },
  "timestamp": "2025-11-14T11:00:00Z"
}
```

---

### 5. Системные сообщения

**Queue**: `/user/queue/system`

**Назначение**: Системные оповещения и ошибки

**Подписка** (Client → Server):
```javascript
stompClient.subscribe('/user/queue/system', (message) => {
  const systemMsg = JSON.parse(message.body);
  handleSystemMessage(systemMsg);
});
```

**Сообщения** (Server → Client):

#### 5.1. Ошибка аутентификации

```json
{
  "type": "AUTH_ERROR",
  "message": "JWT token expired. Please reconnect with a new token.",
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 5.2. Информационное сообщение

```json
{
  "type": "INFO",
  "message": "System maintenance scheduled for 2025-11-15 at 02:00 UTC",
  "timestamp": "2025-11-14T11:00:00Z"
}
```

#### 5.3. Rate Limiting

```json
{
  "type": "RATE_LIMIT",
  "message": "Too many subscriptions. Maximum 50 allowed.",
  "timestamp": "2025-11-14T11:00:00Z"
}
```

---

## Client-to-Server Messages (отправка сообщений)

### Пинг (Heartbeat)

Клиент может отправлять ping для поддержания соединения:

```javascript
stompClient.send('/app/ping', {}, JSON.stringify({ timestamp: Date.now() }));
```

**Ответ** (Server → Client):
```json
{
  "type": "PONG",
  "timestamp": "2025-11-14T11:00:00Z"
}
```

### Пометить уведомление как прочитанное (альтернативный способ)

```javascript
stompClient.send('/app/notifications/mark-read', {}, JSON.stringify({ 
  notificationId: 'uuid'
}));
```

**Примечание**: Рекомендуется использовать REST API для этого действия. WebSocket send используется только для специфичных real-time операций.

---

## Управление подписками

### Автоматическая подписка

При подключении клиент должен автоматически подписаться на:
1. `/user/queue/notifications` - персональные уведомления
2. `/user/queue/system` - системные сообщения

Если пользователь имеет активный таймер (проверка через REST API):
3. `/user/queue/timer` - обновления таймера

### Динамическая подписка

При открытии страницы проекта:
```javascript
const projectSubscription = stompClient.subscribe(`/topic/project.${projectId}`, handler);
```

При закрытии страницы:
```javascript
projectSubscription.unsubscribe();
```

Аналогично для страницы задачи:
```javascript
const taskSubscription = stompClient.subscribe(`/topic/task.${taskId}`, handler);
// ... при закрытии
taskSubscription.unsubscribe();
```

**Ограничение**: Максимум 50 одновременных подписок на пользователя для предотвращения злоупотреблений.

---

## Reconnection Strategy

### Автоматическое переподключение

При разрыве соединения клиент должен реализовать exponential backoff:

```javascript
let reconnectDelay = 1000; // 1 second
const maxReconnectDelay = 30000; // 30 seconds

function connect() {
  const socket = new SockJS(`${WS_URL}?token=${getAccessToken()}`);
  const stompClient = Stomp.over(socket);
  
  stompClient.connect({}, onConnect, onError);
}

function onError(error) {
  console.error('Connection error:', error);
  
  setTimeout(() => {
    reconnectDelay = Math.min(reconnectDelay * 2, maxReconnectDelay);
    connect();
  }, reconnectDelay);
}

function onConnect(frame) {
  console.log('Connected:', frame);
  reconnectDelay = 1000; // reset delay on successful connection
  
  // Re-subscribe to topics
  subscribeToTopics();
}
```

### Обновление токена

Если соединение разорвалось из-за истечения токена:
1. Клиент получает новый access token через refresh token (REST API)
2. Переподключается с новым токеном

---

## Error Handling

### Ошибки подписки

Если подписка на topic/queue не удалась:

```javascript
stompClient.subscribe('/topic/project.invalid-uuid', handler, (error) => {
  console.error('Subscription error:', error);
  // Handle error (например, показать уведомление пользователю)
});
```

### Ошибки отправки

```javascript
stompClient.send(destination, {}, payload, (error) => {
  if (error) {
    console.error('Send error:', error);
  }
});
```

---

## Performance Considerations

### Throttling и Debouncing

Для events, которые происходят часто (например, изменения полей задачи в реальном времени при редактировании несколькими пользователями), сервер применяет throttling:
- Максимум 10 сообщений в секунду на topic
- Batch updates: если изменений много, они группируются и отправляются одним сообщением

### Compression

WebSocket сообщения автоматически сжимаются (WebSocket compression extension `permessage-deflate`) для уменьшения трафика.

---

## Security

### Authorization

После аутентификации (JWT в query parameter), сервер проверяет права доступа перед подпиской:

**Правила**:
- `/user/queue/*`: Доступ только к своим очередям
- `/topic/project.{projectId}`: Доступ только участникам проекта
- `/topic/task.{taskId}`: Доступ только участникам проекта, к которому относится задача

Если пользователь пытается подписаться на topic без прав → сообщение об ошибке в `/user/queue/system`.

### Rate Limiting

- Максимум 50 подписок на пользователя
- Максимум 100 сообщений от клиента в минуту (send)
- При превышении → временный бан на 5 минут

---

## Testing

### WebSocket Testing Tools

1. **Browser Console**:
```javascript
const socket = new SockJS('http://localhost:8080/ws?token=<token>');
const stompClient = Stomp.over(socket);
stompClient.connect({}, () => {
  console.log('Connected');
  stompClient.subscribe('/user/queue/notifications', (msg) => {
    console.log('Notification:', JSON.parse(msg.body));
  });
});
```

2. **Postman**: Supports WebSocket connections

3. **wscat** (CLI tool):
```bash
wscat -c "ws://localhost:8080/ws?token=<token>"
```

### Integration Tests

Backend integration tests используют `WebSocketClient` из Spring Test:

```java
@Test
void shouldReceiveTimerUpdates() {
  WebSocketStompClient stompClient = // ... setup
  StompSession session = stompClient.connect(WS_URL, new StompSessionHandlerAdapter()).get();
  
  session.subscribe("/user/queue/timer", new StompFrameHandler() {
    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
      // Assert payload
    }
  });
  
  // Start timer via REST API
  // Assert timer updates received
}
```

---

## Example: Angular Client Implementation

```typescript
import { RxStompConfig } from '@stomp/rx-stomp';
import * as SockJS from 'sockjs-client';

export const rxStompConfig: RxStompConfig = {
  brokerURL: 'ws://localhost:8080/ws',
  
  // Heartbeat
  heartbeatIncoming: 30000,
  heartbeatOutgoing: 30000,
  
  // Reconnection
  reconnectDelay: 5000,
  
  // WebSocket factory (для передачи токена)
  webSocketFactory: () => {
    const token = this.authService.getAccessToken();
    return new SockJS(`${environment.wsUrl}?token=${token}`);
  },
  
  // Connection callback
  beforeConnect: () => {
    console.log('Connecting to WebSocket...');
  },
  
  // Debug
  debug: (msg: string) => {
    if (!environment.production) {
      console.log(new Date(), msg);
    }
  }
};
```

**Service для подписок**:

```typescript
@Injectable({ providedIn: 'root' })
export class WebSocketService {
  private rxStomp: RxStompService;
  
  constructor(private rxStomp: RxStompService) {}
  
  subscribeToNotifications(): Observable<Notification> {
    return this.rxStomp
      .watch('/user/queue/notifications')
      .pipe(
        map(message => JSON.parse(message.body)),
        map(data => data.data)
      );
  }
  
  subscribeToTimer(): Observable<TimerUpdate> {
    return this.rxStomp
      .watch('/user/queue/timer')
      .pipe(
        map(message => JSON.parse(message.body)),
        map(data => data.data)
      );
  }
  
  subscribeToProject(projectId: string): Observable<ProjectEvent> {
    return this.rxStomp
      .watch(`/topic/project.${projectId}`)
      .pipe(
        map(message => JSON.parse(message.body))
      );
  }
  
  subscribeToTask(taskId: string): Observable<TaskEvent> {
    return this.rxStomp
      .watch(`/topic/task.${taskId}`)
      .pipe(
        map(message => JSON.parse(message.body))
      );
  }
}
```

---

## Summary

WebSocket протокол обеспечивает:
✅ Real-time обновления таймеров (каждую секунду)  
✅ Мгновенную доставку уведомлений  
✅ Синхронизацию изменений в проектах и задачах  
✅ Масштабируемость до 500 одновременных соединений  
✅ Безопасность через JWT аутентификацию и authorization checks  
✅ Надежность через heartbeat и automatic reconnection  

Протокол спроектирован для эффективной работы с реактивным backend (Spring WebFlux) и обеспечивает все real-time требования из спецификации.

