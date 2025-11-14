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
- **Constructor Injection**: Все Spring компоненты используют constructor injection через @RequiredArgsConstructor (Lombok) для immutability и улучшенной testability

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

## 5. State Management на Frontend (NgRx + Signals)

### Решение: Hybrid подход - NgRx для глобального состояния + Angular Signals для локального

**Обоснование**:
- **NgRx для enterprise state**: Проверенный Redux pattern для сложных данных с cross-cutting concerns
- **Signals для UI state**: Эффективное управление локальным состоянием компонентов (Angular 18+)
- **Лучшее из обоих миров**: NgRx DevTools + Signals performance и простота
- **Fine-grained reactivity**: Signals обеспечивают точечные обновления без лишних re-renders
- **Совместимость**: NgRx и Signals прекрасно работают вместе через `toSignal()` и `toObservable()`

**NgRx Store (глобальное состояние)**:
```
AppState (NgRx Store)
├── auth: AuthState (user, token, isAuthenticated)
├── projects: ProjectsState (entities, selectedId, loading)
├── tasks: TasksState (entities, filters, selectedId)
├── timeEntries: TimeEntriesState (entities, activeTimer)
├── notifications: NotificationsState (entities, unreadCount)
└── router: RouterState (current route, params)
```

**Signals (локальное состояние компонентов)**:
- UI state: `sidebarOpen = signal(false)`, `selectedTab = signal('overview')`
- Form state: `isSubmitting = signal(false)`, `validationErrors = signal([])`
- Loading indicators: `isLoading = signal(false)`
- Modal state: `showModal = signal(false)`, `modalData = signal(null)`
- Filters: `searchQuery = signal('')`, `selectedPriority = signal<Priority | null>(null)`

**Интеграция NgRx с Signals**:
```typescript
// В компонентах
readonly user = toSignal(this.store.select(selectCurrentUser));
readonly projects = toSignal(this.store.select(selectAllProjects));

// Computed signals на основе store
readonly hasActiveTimer = computed(() => 
  this.timeEntries()?.some(e => !e.endTime) ?? false
);
```

**Паттерны**:
- **Feature stores (NgRx)**: Каждый feature module имеет свой state slice
- **Facade service**: Скрывает детали NgRx, предоставляет signals API
- **Signal inputs**: `@Input() с transform` для реактивных inputs (Angular 17.1+)
- **Signal-based effects**: NgRx Effects с `toObservable(signal)` для реактивности
- **Optimistic updates**: NgRx для оптимистичных обновлений, Signals для UI feedback

**Архитектура компонентов (Smart/Presentational)**:

**Smart Components (Container)**:
- Управляют бизнес-логикой и состоянием
- Взаимодействуют с NgRx Store и сервисами
- **DI**: `inject()` для сервисов без параметров, constructor для сложных случаев
- Обрабатывают события от Presentational компонентов
- Пример: `TaskListContainerComponent`, `ProjectDashboardContainerComponent`
- Расположение: `features/{feature}/containers/`

**Presentational Components (Dumb)**:
- Получают данные через `@Input()` (preferably signals)
- Эмитят события через `@Output()`
- Не знают о NgRx Store или сервисах
- Чистые, переиспользуемые, легко тестируемые
- **OnPush change detection** для оптимизации производительности
- Пример: `TaskCardComponent`, `TaskFormComponent`, `TimerDisplayComponent`
- Расположение: `features/{feature}/components/` или `shared/components/`

**Пример структуры**:
```
features/tasks/
├── containers/
│   ├── task-list-container.component.ts    # Smart: Store + Logic
│   └── task-details-container.component.ts # Smart: Store + Logic
├── components/
│   ├── task-card.component.ts              # Dumb: @Input/@Output
│   ├── task-form.component.ts              # Dumb: ReactiveForm
│   └── task-filters.component.ts           # Dumb: @Input/@Output
└── store/
    ├── tasks.actions.ts
    ├── tasks.reducer.ts
    └── tasks.selectors.ts
```

**Dependency Injection (Hybrid подход)**:

**Используем `inject()` (Angular 14+)**:
```typescript
@Component({...})
export class TaskListContainerComponent {
  // Простые сервисы без параметров - inject()
  private store = inject(Store);
  private tasksFacade = inject(TasksFacade);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  
  // Signals из store
  readonly tasks = toSignal(this.store.select(selectAllTasks));
  readonly loading = toSignal(this.store.select(selectTasksLoading));
}
```

**Используем constructor injection**:
```typescript
@Injectable()
export class TasksApiService {
  // Когда нужны параметры или сложная логика в конструкторе
  constructor(
    private http: HttpClient,
    @Inject(API_BASE_URL) private apiUrl: string,
    @Optional() private logger?: LoggerService
  ) {
    this.logger?.info('TasksApiService initialized');
  }
}
```

**Преимущества hybrid подхода**:
- `inject()` упрощает код компонентов (меньше boilerplate)
- `inject()` позволяет DI в functional context (guards, interceptors)
- Constructor явно показывает обязательные зависимости с параметрами
- Соответствует Angular best practices и future direction

**Change Detection Strategy**:

**OnPush для Presentational компонентов**:
```typescript
@Component({
  selector: 'app-task-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `...`
})
export class TaskCardComponent {
  @Input() task!: Task;  // Immutable inputs
  @Output() edit = new EventEmitter<Task>();
  
  // Signals автоматически работают с OnPush
  isExpanded = signal(false);
}
```

**Default для Smart компонентов (при необходимости)**:
```typescript
@Component({
  selector: 'app-task-list-container',
  // changeDetection: ChangeDetectionStrategy.Default (по умолчанию)
  template: `...`
})
export class TaskListContainerComponent {
  // Async операции, WebSocket updates могут требовать Default
  // Signals из store с toSignal() работают с обеими стратегиями
  readonly tasks = toSignal(this.store.select(selectAllTasks));
}
```

**Правила для OnPush**:
- Все @Input должны быть immutable (или использовать OnChanges)
- Signals автоматически триггерят change detection в OnPush
- Async pipe автоматически работает с OnPush
- События (@Output, DOM events) триггерят change detection
- Избегать mutable операций над @Input данными

**Performance преимущества**:
- OnPush компоненты проверяются только при изменении inputs или событиях
- Signals обеспечивают fine-grained reactivity без полной проверки дерева
- Сокращение проверок change detection на 60-80% для больших списков
- Критично для оптимизации task lists, календаря, таблиц времени

**Reactive Forms (для всех форм)**:

**Обоснование выбора**:
- **Type safety**: TypeScript типы для FormControl, FormGroup, FormArray
- **Testability**: Тестирование логики форм без DOM
- **Программный контроль**: Динамическое управление валидаторами и значениями
- **Композиция**: Nested FormGroups, FormArray для динамических списков
- **Reactive validation**: Асинхронные валидаторы, cross-field validation
- **NgRx интеграция**: Легкое сохранение состояния форм в store

**Пример использования**:
```typescript
@Component({
  selector: 'app-task-form',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TaskFormComponent implements OnInit {
  private fb = inject(FormBuilder);
  
  taskForm = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    projectId: [null as number | null, Validators.required],
    assigneeId: [null as number | null],
    status: ['NEW' as TaskStatus, Validators.required],
    priority: ['MEDIUM' as TaskPriority, Validators.required],
    deadline: [null as Date | null],
    estimatedHours: [null as number | null, [Validators.min(0), Validators.max(999)]],
    labels: this.fb.array<number>([]),  // FormArray для меток
    parentTaskId: [null as number | null]
  });
  
  // Custom validator для бизнес-правил
  get labels(): FormArray {
    return this.taskForm.get('labels') as FormArray;
  }
  
  addLabel(labelId: number): void {
    this.labels.push(this.fb.control(labelId));
  }
  
  // Async validator для проверки уникальности
  titleValidator(): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      return this.tasksService.checkTitleUnique(control.value).pipe(
        map(isUnique => isUnique ? null : { titleExists: true }),
        catchError(() => of(null))
      );
    };
  }
}
```

**Формы в приложении**:
- **Task Form**: создание/редактирование задач (nested forms для подзадач)
- **Project Form**: создание/редактирование проектов
- **Time Entry Form**: ручное добавление времени (date/time pickers, validation)
- **User Settings Form**: настройки уведомлений (toggle groups)
- **Filter Forms**: фильтры задач (динамические FormArray для множественных условий)
- **Login/Register Forms**: аутентификация (async validators для email)

**Валидация**:
- Built-in validators: `required`, `minLength`, `maxLength`, `email`, `pattern`
- Custom validators: уникальность, бизнес-правила, cross-field validation
- Async validators: проверка на бэкенде (debounce для производительности)
- Error messages: централизованная мапа ошибок с i18n

**Интеграция с Signals**:
```typescript
// Reactive form values как signal
readonly formValue = toSignal(this.taskForm.valueChanges);
readonly isFormValid = toSignal(this.taskForm.statusChanges.pipe(
  map(status => status === 'VALID')
));
```

**Рассмотренные альтернативы**:
- **Только NgRx**: Verbose для простого UI state, всё через store - избыточно
- **Только Signals**: Нет DevTools, сложнее для enterprise patterns (effects, entity management)
- **Akita**: Меньше поддержки, не интегрирована с Signals нативно

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

**Logging Strategy**:
- **SLF4J с Logback**: Стандартная конфигурация Spring Boot
- **Уровни логирования**:
  - **ERROR**: Исключения и критические ошибки
  - **WARN**: Предупреждения (retry попытки, deprecated API usage)
  - **INFO**: Бизнес-операции (создание задачи, старт/стоп таймера, изменение статуса, аутентификация)
  - **DEBUG**: Детальная информация для development (SQL queries, method входы/выходы, reactive operators)
- **Production**: INFO уровень по умолчанию, DEBUG для конкретных пакетов при troubleshooting
- **Development**: DEBUG уровень для com.sprinto.tms, INFO для фреймворков
- **Structured logging**: JSON формат в production для парсинга ELK/Splunk
- **Correlation ID**: MDC context для трейсинга запросов через систему

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
   - **Backend**: Bean Validation (JSR-380) для всех DTO
     - Аннотации: @NotNull, @NotBlank, @Size, @Email, @Min, @Max, @Pattern
     - @Valid на параметрах контроллеров для автоматической валидации
     - Custom validators для бизнес-правил (например, @UniqueEmail)
     - Группы валидации для разных сценариев (Create, Update)
     - MethodValidationPostProcessor для валидации параметров сервисов
   - **Frontend**: Angular Reactive Forms с validators
   - Sanitization: XSS protection через Content Security Policy

2.1. **Exception Handling**:
   - **@ControllerAdvice**: Глобальная обработка всех исключений приложения
   - **@ExceptionHandler**: Методы для каждого типа исключения (ValidationException, NotFoundException, AccessDeniedException и т.д.)
   - **ErrorResponse DTO**: Унифицированный формат ответа с timestamp, status, error, message, path, validationErrors
   - **Reactive Exception Handling**: Обработка ошибок в реактивных потоках через onErrorResume/onErrorMap

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

