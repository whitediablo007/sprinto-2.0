# Исследование и технические решения

**Проект**: Система управления проектами и задачами для малых команд  
**Дата**: 2025-11-14  
**Статус**: Завершено

## Цель исследования

Определить оптимальные архитектурные решения, паттерны и практики для реализации SPA-системы управления проектами с требованиями к real-time обновлениям, высокой производительности и реактивной обработке данных.

---

## 1. Архитектура приложения

### Решение: Микросервисная монолитная архитектура (Modular Monolith)

**Обоснование**:
- Для команд 5-15 человек и нагрузки до 100 одновременных пользователей полноценная микросервисная архитектура избыточна
- Модульный монолит обеспечивает четкое разделение доменов (проекты, задачи, время, уведомления) с возможностью выделения в микросервисы при необходимости
- Упрощает deployment, debugging и поддержку на начальном этапе
- Все модули работают в одном процессе Spring Boot, используя общую R2DBC connection pool

**Рассмотренные альтернативы**:
- **Полная микросервисная архитектура**: Отклонена из-за избыточной сложности для целевого масштаба, необходимости distributed tracing, service discovery, что увеличивает операционные затраты
- **Традиционный монолит без модулей**: Отклонен из-за риска превращения в "big ball of mud", сложности масштабирования команды разработки

**Ключевые модули**:
1. **User & Auth Module**: Аутентификация, авторизация, управление пользователями
2. **Project Module**: Управление проектами, участниками, настройками
3. **Task Module**: CRUD задач, иерархия, зависимости, статусы
4. **Time Tracking Module**: Таймеры, записи времени, биллинг
5. **Notification Module**: WebSocket, email, push уведомления
6. **Reporting Module**: Генерация отчетов, аналитика, экспорт
7. **Integration Module**: Google Calendar, внешние API

---

## 2. Реактивный стек (Spring WebFlux + R2DBC)

### Решение: Полностью реактивный стек с Project Reactor

**Обоснование**:
- **Non-blocking I/O**: Критично для поддержки 100+ одновременных пользователей с WebSocket соединениями
- **Backpressure handling**: Автоматическое управление потоком данных предотвращает перегрузку
- **Эффективное использование ресурсов**: Реактивный подход использует меньше потоков (event loop model vs thread-per-request)
- **Естественная интеграция с WebSocket**: Spring WebFlux предоставляет first-class поддержку WebSocket

**R2DBC vs JDBC**:
- R2DBC обеспечивает неблокирующий доступ к PostgreSQL, сохраняя реактивность всего стека
- Flyway для миграций (работает на JDBC, но запускается при старте приложения)

**Рассмотренные альтернативы**:
- **Spring MVC + JDBC**: Проще в освоении, но блокирующий I/O не обеспечит требуемую производительность при real-time обновлениях таймеров и уведомлений для 100 пользователей
- **Смешанный подход (WebFlux + JDBC через blocking context)**: Создает bottleneck, теряет преимущества реактивности

**Вызовы и решения**:
- **Крутая кривая обучения**: Требуется обучение команды работе с Reactor (Mono, Flux, операторы)
- **Debugging**: Используем Reactor Hooks для readable stack traces в dev режиме
- **Транзакции**: R2DBC TransactionalOperator для управления транзакциями в реактивном стиле

---

## 3. Аутентификация и безопасность

### Решение: JWT-based authentication с Spring Security Reactive

**Обоснование**:
- **Stateless**: JWT токены позволяют не хранить сессии на сервере, что критично для масштабирования
- **WebSocket compatibility**: JWT можно передавать в WebSocket handshake для аутентификации соединения
- **Spring Security 6.x**: Native поддержка реактивного стека (ReactiveSecurityContextHolder)

**Архитектура**:
1. **Login endpoint** (`POST /api/auth/login`): Принимает email/пароль, возвращает JWT access token и refresh token
2. **Access Token**: Short-lived (15 минут), содержит userId, roles, permissions
3. **Refresh Token**: Long-lived (7 дней), хранится в httpOnly cookie для безопасности
4. **Token Refresh endpoint** (`POST /api/auth/refresh`): Обновляет access token по refresh token
5. **JWT Filter**: Валидирует токен в каждом запросе, устанавливает SecurityContext

**Хранение паролей**: BCrypt с cost factor 12 (балансирует безопасность и производительность)

**Рассмотренные альтернативы**:
- **Session-based auth**: Отклонена из-за необходимости хранить сессии (Redis или in-memory), что усложняет архитектуру и создает stateful зависимость
- **OAuth 2.0 провайдеры**: Рассмотрено для будущих версий, но для MVP email/пароль достаточно согласно спецификации (FR-007)

**Password Recovery Flow**:
1. Пользователь запрашивает сброс пароля → генерируется temporary token (UUID)
2. Token сохраняется в БД с expiration (1 час)
3. Email с ссылкой отправляется пользователю
4. Пользователь переходит по ссылке, устанавливает новый пароль
5. Token инвалидируется после использования

---

## 4. WebSocket для real-time обновлений

### Решение: Spring WebFlux WebSocket с STOMP protocol

**Обоснование**:
- **STOMP over WebSocket**: Предоставляет message broker паттерн с topics/queues
- **Pub/Sub модель**: Упрощает отправку уведомлений группам пользователей (проект, задача)
- **Automatic reconnection**: Frontend библиотека (SockJS + RxStomp) обеспечивает reconnection при обрыве соединения
- **Spring Integration**: Native поддержка в Spring WebFlux

**Темы WebSocket**:
- `/topic/timer.{userId}`: Обновления активного таймера пользователя (каждую секунду)
- `/topic/notifications.{userId}`: Персональные уведомления
- `/topic/project.{projectId}`: События проекта (новые задачи, изменения)
- `/topic/task.{taskId}`: Изменения конкретной задачи (статус, комментарии)

**Масштабирование**:
- Для одного backend instance: In-memory message broker достаточно для 100 пользователей
- Для горизонтального масштабирования: Интеграция с Redis Pub/Sub или RabbitMQ

**Heartbeat**: Ping/Pong каждые 30 секунд для определения разорванных соединений

**Рассмотренные альтернативы**:
- **Server-Sent Events (SSE)**: Односторонняя коммуникация (сервер → клиент), не подходит для interactive features
- **Polling**: Высокая нагрузка на сервер, задержки в обновлениях, неэффективно

---

## 5. State Management на Frontend (NgRx)

### Решение: NgRx (Redux pattern для Angular) с Entity adapter

**Обоснование**:
- **Предсказуемое состояние**: Single source of truth для всего состояния приложения
- **Time-travel debugging**: Redux DevTools для отладки изменений состояния
- **Отделение side effects**: NgRx Effects для асинхронных операций (API calls, WebSocket)
- **Селекторы с мемоизацией**: Эффективное чтение данных без пересчетов
- **Entity adapter**: Готовые редьюсеры для CRUD операций с нормализованными данными

**Структура Store**:
```
AppState
├── auth: AuthState (user, token, isAuthenticated)
├── projects: ProjectsState (entities, selectedId, loading)
├── tasks: TasksState (entities, filters, selectedId)
├── timeEntries: TimeEntriesState (entities, activeTimer)
├── notifications: NotificationsState (entities, unreadCount)
├── ui: UIState (sidebar, modals, loading indicators)
└── router: RouterState (current route, params)
```

**Паттерны**:
- **Feature stores**: Каждый feature module имеет свой state slice
- **Facade service**: Скрывает детали NgRx от компонентов, предоставляет простой API
- **Optimistic updates**: Для drag & drop операций (обновление UI до ответа сервера)

**Рассмотренные альтернативы**:
- **Сервисы с BehaviorSubject**: Простота, но плохая масштабируемость, нет time-travel debugging, сложно управлять сложным состоянием
- **Akita**: Менее verbose чем NgRx, но меньше community support и меньше инструментов

---

## 6. Модель данных и схема БД

### Решение: Реляционная модель с PostgreSQL

**Обоснование**:
- **ACID транзакции**: Критично для корректности данных (биллинг, время)
- **Сложные запросы**: Необходимы JOIN для отчетов, аналитики
- **JSON support**: PostgreSQL JSONB для гибких полей (настройки, metadata)
- **Full-text search**: Для поиска по задачам, проектам
- **Зрелость**: Стабильность, производительность, богатая экосистема

**Ключевые таблицы**:
1. **users**: id, email, password_hash, name, avatar_url, hourly_rate, created_at
2. **projects**: id, name, description, color, owner_id, status, hourly_rate, start_date, end_date
3. **project_members**: project_id, user_id, role (enum), joined_at
4. **tasks**: id, project_id, parent_task_id, title, description, assignee_id, status, priority, deadline, estimated_hours, created_by, created_at
5. **task_dependencies**: dependent_task_id, blocking_task_id
6. **time_entries**: id, task_id, user_id, start_time, end_time, duration, hourly_rate, cost, entry_type (timer/manual), deleted_at
7. **time_entry_audit**: entry_id, changed_by, changed_at, change_reason, old_values
8. **tags**: id, name, color, project_id (nullable for global tags)
9. **task_tags**: task_id, tag_id
10. **comments**: id, task_id, user_id, text, mentioned_users (jsonb), created_at
11. **notifications**: id, user_id, type, title, message, related_entity_type, related_entity_id, read_at, created_at
12. **files**: id, task_id, filename, file_size, mime_type, storage_path, uploaded_by, uploaded_at
13. **calendar_settings**: user_id, google_calendar_id, access_token, refresh_token, sync_filters (jsonb)

**Индексы**:
- Составные индексы: (project_id, status), (assignee_id, status), (user_id, start_time)
- B-tree индексы на foreign keys
- GiST индекс на full-text search columns

**Soft delete**: Для time_entries (deleted_at nullable timestamp)

**Рассмотренные альтернативы**:
- **NoSQL (MongoDB)**: Гибкая схема, но слабые транзакции, сложные JOIN для отчетов
- **Time-series DB (TimescaleDB)**: Оптимизирована для time_entries, но излишня сложность для остальных данных

---

## 7. Интеграция с Google Calendar

### Решение: Google Calendar API v3 с OAuth 2.0

**Обоснование**:
- **Официальный API**: Стабильность, документация, client libraries
- **OAuth 2.0**: Стандарт авторизации, пользователь контролирует доступ
- **Incremental authorization**: Запрашиваем доступ к календарю только когда пользователь активирует интеграцию

**Flow**:
1. Пользователь нажимает "Подключить Google Calendar"
2. Frontend открывает OAuth 2.0 popup (Google Consent Screen)
3. После авторизации Google редиректит на callback URL с authorization code
4. Backend обменивает code на access_token и refresh_token
5. Токены сохраняются в `calendar_settings` таблице (encrypted)
6. Backend использует refresh_token для получения нового access_token при истечении

**Синхронизация**:
- **Scheduled job** (Spring @Scheduled): Каждые 15 минут проверяет задачи с дедлайнами
- **Event-driven**: При создании/обновлении задачи с дедлайном → немедленный экспорт
- **Фильтры**: Применяются на уровне service layer перед экспортом

**Retry strategy**: Exponential backoff при ошибках API (rate limiting, network issues)

**Рассмотренные альтернативы**:
- **Webhook от Google**: Сложность настройки, требует публичный endpoint, overkill для односторонней синхронизации

---

## 8. Отчеты и экспорт данных

### Решение: Server-side генерация с Apache POI

**Обоснование**:
- **Apache POI**: Зрелая библиотека для генерации Excel (.xlsx)
- **Server-side**: Генерация на backend обеспечивает корректность данных, доступ к БД
- **Streaming**: POI Streaming API для больших отчетов без OutOfMemory
- **Reactive integration**: Wrap синхронного POI кода в `Mono.fromCallable(() -> ...).subscribeOn(Schedulers.boundedElastic())`

**Типы экспорта**:
1. **Excel**: Apache POI для .xlsx (отчеты с форматированием, графики)
2. **CSV**: Простой text streaming для raw data
3. **PDF**: (Опционально в будущем) iText library

**Генерация графиков в отчете**:
- Фронтенд отправляет запрос на генерацию отчета с параметрами
- Backend агрегирует данные из БД
- Данные для графиков включаются в Excel как embedded charts (POI Chart API)

**Рассмотренные альтернативы**:
- **Client-side генерация** (SheetJS): Не имеет доступа к полным данным БД, требует передачи больших объемов данных на frontend
- **Внешний reporting service** (Jasper Reports, Crystal Reports): Overkill для MVP, дополнительная инфраструктура

---

## 9. Уведомления: Multi-channel delivery

### Решение: Unified Notification Service с routing по каналам

**Архитектура**:
```
Event (например, task assigned) 
→ NotificationService.createNotification() 
→ Routing на основе user preferences 
→ Channel handlers (WebSocket, Email, Push)
```

**Каналы**:

1. **In-app (WebSocket)**:
   - Немедленная доставка через `/topic/notifications.{userId}`
   - Хранение в БД (таблица notifications) для истории
   - Badge count обновляется в UI через NgRx store

2. **Email**:
   - **Spring Boot Mail Starter** с SMTP или SendGrid API
   - **Template engine**: Thymeleaf для HTML email templates
   - **Async sending**: `@Async` для неблокирующей отправки
   - **Batching**: Группировка уведомлений за период (опционально)

3. **Push (Browser)**:
   - **Web Push API**: Стандарт W3C для push notifications
   - **VAPID keys**: Для аутентификации push messages
   - Frontend запрашивает permission, регистрирует service worker
   - Backend отправляет push через Web Push Protocol
   - **Library**: web-push Java library

**User preferences**:
- Хранятся в JSONB поле в таблице users
- Структура: `{ "taskAssigned": { "inApp": true, "email": true, "push": false }, ... }`
- Проверяются перед routing уведомления

**"Не беспокоить" режим**:
- Флаг `do_not_disturb_until` в таблице users
- NotificationService проверяет флаг перед отправкой email/push
- In-app уведомления сохраняются, но не показываются как pop-up (silent delivery)

**Рассмотренные альтернативы**:
- **Третий-party service** (Firebase Cloud Messaging, OneSignal): Vendor lock-in, дополнительные затраты, но проще в setup (рассмотреть для production)

---

## 10. Файловое хранилище

### Решение: Локальное хранилище с возможностью миграции на S3-compatible storage

**MVP подход** (Phase 1):
- **Локальная файловая система**: Файлы сохраняются в директорию `{UPLOAD_DIR}/{projectId}/{taskId}/{filename}`
- **Metadata в БД**: Таблица files хранит path, size, mime_type
- **Ограничения**: Max 10MB на файл, проверка MIME types на сервере

**Загрузка**:
```
POST /api/tasks/{taskId}/files
Content-Type: multipart/form-data
```
- Spring WebFlux DataBufferUtils для streaming upload (reactive, не загружает файл целиком в память)
- Генерация UUID для имени файла (предотвращение коллизий)
- Валидация: размер, MIME type, file extension

**Скачивание**:
```
GET /api/files/{fileId}
```
- Streaming download с правильным Content-Type header
- Authorization check: пользователь имеет доступ к задаче

**Будущее масштабирование**:
- Интерфейс `FileStorageService` с реализациями: `LocalFileStorage`, `S3FileStorage`
- Миграция на AWS S3, MinIO или Google Cloud Storage при необходимости

**Рассмотренные альтернативы**:
- **Сразу S3**: Усложняет локальную разработку, требует AWS credentials, дополнительные затраты для MVP
- **Хранение в БД (bytea)**: Увеличивает размер БД, медленнее для больших файлов, затрудняет backup

---

## 11. Тестирование

### Решение: Многоуровневая стратегия тестирования

**Backend**:

1. **Unit тесты** (JUnit 5 + Mockito):
   - Сервисы: бизнес-логика с mock репозиториями
   - Mappers: DTO ↔ Entity conversion
   - Утилиты: чистые функции
   - Coverage goal: 80%+

2. **Integration тесты** (Spring Boot Test + Testcontainers):
   - R2DBC репозитории с реальной PostgreSQL в Docker
   - API endpoints с MockMvc (WebFlux Test)
   - WebSocket handlers с WebSocketClient
   - Testcontainers запускает PostgreSQL перед тестами

3. **Contract тесты**:
   - Проверка соответствия endpoints OpenAPI спецификации
   - Валидация request/response schemas

**Frontend**:

1. **Unit тесты** (Jasmine + Karma):
   - Компоненты: изолированное тестирование с mock сервисами
   - Services: HTTP calls с HttpClientTestingModule
   - Pipes, Directives: pure logic
   - NgRx: reducers (pure functions), effects (mock actions/services), selectors

2. **Integration тесты**:
   - Взаимодействие компонентов с реальными сервисами (mock backend)
   - Routing: навигация между страницами

3. **E2E тесты** (Cypress):
   - Critical user flows:
     - Создание проекта → добавление задачи → запуск таймера
     - Генерация отчета → экспорт Excel
     - Назначение задачи → получение уведомления
   - Visual regression (Cypress Percy plugin - опционально)

**CI/CD**:
- GitHub Actions / GitLab CI
- Pipeline: lint → unit tests → integration tests → build → E2E tests → deploy

---

## 12. Производительность и оптимизация

### Решение: Комбинация техник

**Backend**:

1. **Кэширование**:
   - **Spring Cache** с Caffeine (in-memory cache)
   - Кэшируются: user permissions, project members, tags
   - TTL: 5-15 минут
   - Invalidation: при изменениях через event bus

2. **Database оптимизация**:
   - **Индексы**: На часто запрашиваемые поля (см. раздел 6)
   - **Connection pooling**: R2DBC connection pool (max 20 connections)
   - **Pagination**: Для списков задач, отчетов (limit/offset или cursor-based)
   - **Batch operations**: Bulk insert для миграций, импорта

3. **Query optimization**:
   - **N+1 problem**: Решается через JOIN или batch loading
   - **Projection**: Выбираем только нужные поля в DTO
   - **Reactive streams**: Backpressure предотвращает перегрузку

**Frontend**:

1. **Bundle optimization**:
   - **Lazy loading**: Feature modules загружаются по требованию
   - **Tree shaking**: Webpack удаляет неиспользуемый код
   - **Code splitting**: Vendor bundle отдельно от app bundle
   - **AOT compilation**: Ahead-of-time для production build
   - Target: Initial bundle <2MB gzipped

2. **Runtime optimization**:
   - **OnPush change detection**: Для компонентов с immutable state
   - **Virtual scrolling** (Angular CDK): Для длинных списков задач
   - **TrackBy**: В ngFor для оптимизации re-rendering
   - **Memoization**: Селекторы NgRx кэшируют результаты
   - **Debounce/Throttle**: Для search, filter inputs

3. **Asset optimization**:
   - **Image optimization**: WebP формат, lazy loading
   - **Icon fonts**: PrimeIcons минимизированы
   - **CSS**: Tailwind CSS purge для удаления неиспользуемых классов

**Monitoring** (для production):
- Backend: Spring Boot Actuator + Prometheus metrics
- Frontend: Google Analytics, Sentry для error tracking
- APM: Elastic APM или New Relic (опционально)

---

## 13. Безопасность

### Решение: Defense in depth approach

**Уровни защиты**:

1. **Authentication & Authorization**:
   - JWT токены с коротким TTL
   - Refresh token rotation
   - Spring Security Method Security (`@PreAuthorize`)
   - Role-based и permission-based access control

2. **Input validation**:
   - **Backend**: Bean Validation (JSR-380) для DTO
   - **Frontend**: Angular Reactive Forms с validators
   - Sanitization: XSS protection через Content Security Policy

3. **SQL Injection prevention**:
   - R2DBC parameterized queries (защита by design)
   - ORM (Spring Data R2DBC) предотвращает SQL injection

4. **CORS**:
   - Настроенные allowed origins (только frontend domain)
   - Credentials: true для cookie-based auth

5. **Rate limiting**:
   - Bucket4j library для API rate limiting
   - Ограничения: 100 req/min на пользователя для критичных endpoints
   - WebSocket: limit на количество subscriptions

6. **HTTPS**:
   - Mandatory в production
   - HSTS header для enforce HTTPS

7. **Secure headers**:
   - X-Content-Type-Options: nosniff
   - X-Frame-Options: DENY
   - Content-Security-Policy: strict policy

8. **Secrets management**:
   - Environment variables для конфиденциальных данных
   - Spring Cloud Config или Vault для production
   - Google Calendar tokens encrypted в БД (AES-256)

9. **Audit logging**:
   - Логирование всех изменений time_entries (таблица time_entry_audit)
   - Логирование security events (failed logins, permission denials)

---

## 14. Локализация и интернационализация (i18n)

### Решение: Angular i18n с возможностью расширения

**MVP** (Phase 1):
- **Русский язык** как основной (согласно спецификации)
- Angular i18n механизм с JSON translation files
- Структура: `src/assets/i18n/ru.json`, `src/assets/i18n/en.json` (для будущего)

**Backend**:
- API возвращает данные без локализации (dates в ISO 8601, numbers без форматирования)
- Frontend отвечает за отображение согласно locale

**Date/Time**:
- Backend хранит UTC timestamps
- Frontend конвертирует в user timezone (настройка в профиле пользователя)
- Angular DatePipe с locale: ru-RU

**Будущее**:
- Добавление английского и других языков через ngx-translate
- User preference для выбора языка

---

## Резюме ключевых решений

| Аспект | Решение | Обоснование |
|--------|---------|-------------|
| Архитектура | Modular Monolith | Баланс простоты и масштабируемости для команд 5-15 человек |
| Backend | Spring Boot 3 + WebFlux + R2DBC | Реактивность для real-time, высокая производительность |
| Frontend | Angular 18 + NgRx | Enterprise-ready, предсказуемое состояние |
| БД | PostgreSQL 15+ с R2DBC | ACID транзакции, сложные запросы, зрелость |
| Auth | JWT-based (access + refresh tokens) | Stateless, WebSocket совместимость |
| Real-time | WebSocket (STOMP) | Pub/Sub модель для уведомлений и таймеров |
| Файлы | Локальная ФС (MVP) → S3 (future) | Простота для MVP, гибкость для масштабирования |
| Тесты | Multi-level (Unit, Integration, E2E) | Уверенность в качестве кода |
| Кэширование | Caffeine (in-memory) | Снижение нагрузки на БД для часто читаемых данных |
| API Docs | SpringDoc OpenAPI 3 | Автоматическая генерация, Swagger UI |

---

## Риски и митигации

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| Сложность реактивного стека | Средняя | Высокое | Обучение команды, code reviews, паттерны и best practices |
| Масштабирование WebSocket | Низкая | Среднее | In-memory broker для MVP, Redis Pub/Sub для масштабирования |
| Google Calendar API rate limits | Средняя | Низкое | Retry с exponential backoff, кэширование, batch операции |
| Производительность отчетов | Средняя | Среднее | Индексы БД, pagination, async генерация, кэширование результатов |
| Сложность NgRx для простых UI | Низкая | Низкое | Facade сервисы скрывают сложность, документация паттернов |

---

## Следующие шаги

1. ✅ **Phase 0 завершена**: Технические решения определены
2. **Phase 1**: Создание data-model.md, contracts/, quickstart.md
3. **Phase 2**: Разбивка на задачи (/speckit.tasks)
4. **Реализация**: Следуя плану и best practices

---

**Статус исследования**: ✅ Завершено  
**Готовность к Phase 1**: ✅ Да

