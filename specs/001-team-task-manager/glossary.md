# Глоссарий терминов: Team Task Manager

**Проект**: Team Task Manager  
**Ветка**: `001-team-task-manager`  
**Версия**: 1.0.0  
**Дата создания**: 2025-11-16

---

## Цель документа

Этот глоссарий определяет унифицированную терминологию для использования в коде, документации и пользовательском интерфейсе. Цель - обеспечить консистентность naming conventions across backend (Java), frontend (TypeScript/Angular), базы данных (PostgreSQL) и API контрактов.

---

## Принципы именования

### Backend (Java)
- **Классы**: PascalCase (e.g., `TaskService`, `TimeEntry`)
- **Методы**: camelCase (e.g., `startTimer`, `addMember`)
- **Константы**: UPPER_SNAKE_CASE (e.g., `MAX_FILE_SIZE`)
- **Пакеты**: lowercase (e.g., `com.sprinto.tms.domain.task`)

### Frontend (TypeScript/Angular)
- **Интерфейсы**: PascalCase (e.g., `Task`, `ProjectMember`)
- **Компоненты**: PascalCase + суффикс Component (e.g., `TaskListComponent`)
- **Сервисы**: PascalCase + суффикс Service (e.g., `TasksApiService`)
- **Переменные**: camelCase (e.g., `activeTimer`, `projectList`)

### База данных (PostgreSQL)
- **Таблицы**: snake_case, множественное число (e.g., `tasks`, `time_entries`, `project_members`)
- **Колонки**: snake_case (e.g., `user_id`, `created_at`, `billing_rate`)
- **Индексы**: `idx_<table>_<column>` (e.g., `idx_tasks_status`)
- **Foreign Keys**: `fk_<table>_<ref_table>` (e.g., `fk_tasks_projects`)

### REST API
- **Endpoints**: kebab-case (e.g., `/api/time-entries`, `/api/projects/{id}/members`)
- **Query параметры**: camelCase (e.g., `?startDate=`, `?includeArchived=`)
- **HTTP методы**: Стандартные (GET, POST, PUT, DELETE, PATCH)

### WebSocket Topics
- **Topics**: kebab-case с точечной нотацией (e.g., `/topic/project.{projectId}`, `/user/queue/timer`)
- **Message Types**: UPPER_SNAKE_CASE (e.g., `TIMER_UPDATE`, `MEMBER_ADDED`)

---

## Core Entities (Основные сущности)

### User (Пользователь)

**Backend**: `User`  
**Frontend**: `User`  
**Database**: `users`  
**API**: `/api/users`

**Атрибуты**:
- `userId` / `user_id` - Уникальный идентификатор
- `email` - Email адрес (логин)
- `passwordHash` / `password_hash` - Хеш пароля (BCrypt)
- `firstName` / `first_name` - Имя
- `lastName` / `last_name` - Фамилия
- `role` - Системная роль (ADMIN, USER)
- `createdAt` / `created_at` - Дата создания

**Связанные термины**:
- **Authentication** (Аутентификация) - процесс проверки личности
- **Authorization** (Авторизация) - проверка прав доступа
- **JWT Token** - JSON Web Token для аутентификации

---

### Project (Проект)

**Backend**: `Project`  
**Frontend**: `Project`  
**Database**: `projects`  
**API**: `/api/projects`

**Атрибуты**:
- `projectId` / `project_id` - Уникальный идентификатор
- `name` - Название проекта (max 100 символов)
- `description` - Описание проекта (rich text)
- `color` - Цвет проекта (hex, e.g., `#FF5733`)
- `startDate` / `start_date` - Дата начала
- `endDate` / `end_date` - Дата окончания (nullable)
- `billingRate` / `billing_rate` - Часовая ставка (DECIMAL, nullable)
- `currency` - Валюта (enum: USD, EUR, RUB)
- `status` - Статус (enum: ACTIVE, ARCHIVED)
- `ownerId` / `owner_id` - ID владельца проекта

**Связанные термины**:
- **Project Owner** (Владелец проекта) - пользователь с ролью PROJECT_OWNER
- **Project Member** (Участник проекта) - пользователь, добавленный в проект
- **Archived Project** (Архивный проект) - проект со статусом ARCHIVED (read-only)

---

### ProjectMember (Участник проекта)

**Backend**: `ProjectMember`  
**Frontend**: `ProjectMember`  
**Database**: `project_members`  
**API**: `/api/projects/{id}/members`

**Атрибуты**:
- `projectMemberId` / `project_member_id` - Уникальный идентификатор
- `projectId` / `project_id` - ID проекта (FK)
- `userId` / `user_id` - ID пользователя (FK)
- `role` - Роль в проекте (enum: PROJECT_OWNER, PROJECT_MANAGER, DEVELOPER, VIEWER)
- `joinedAt` / `joined_at` - Дата добавления в проект

**Связанные термины**:
- **Role** (Роль) - уровень доступа участника в проекте
- **Permissions** (Права доступа) - разрешения на действия (CRUD)

---

### Task (Задача)

**Backend**: `Task`  
**Frontend**: `Task`  
**Database**: `tasks`  
**API**: `/api/tasks`

**Атрибуты**:
- `taskId` / `task_id` - Уникальный идентификатор
- `projectId` / `project_id` - ID проекта (FK)
- `parentTaskId` / `parent_task_id` - ID родительской задачи (nullable, для иерархии)
- `name` - Название задачи (max 255 символов)
- `description` - Описание задачи (rich text, Quill.js)
- `status` - Статус (enum: NEW, IN_PROGRESS, TESTING, COMPLETED, BLOCKED)
- `priority` - Приоритет (enum: LOW, MEDIUM, HIGH, URGENT)
- `assigneeId` / `assignee_id` - ID исполнителя (FK, nullable)
- `estimatedHours` / `estimated_hours` - Оценка времени (DECIMAL, nullable)
- `dueDate` / `due_date` - Срок выполнения (nullable)
- `createdAt` / `created_at` - Дата создания
- `updatedAt` / `updated_at` - Дата обновления
- `completedAt` / `completed_at` - Дата завершения (nullable)

**Связанные термины**:
- **Subtask** (Подзадача) - задача с заполненным `parentTaskId`
- **Task Hierarchy** (Иерархия задач) - древовидная структура (макс. 5 уровней)
- **Assignee** (Исполнитель) - пользователь, назначенный на задачу
- **Overdue Task** (Просроченная задача) - статус != COMPLETED и `dueDate` < now

---

### TimeEntry (Запись времени)

**Backend**: `TimeEntry`  
**Frontend**: `TimeEntry`  
**Database**: `time_entries`  
**API**: `/api/time-entries`

**Атрибуты**:
- `timeEntryId` / `time_entry_id` - Уникальный идентификатор
- `taskId` / `task_id` - ID задачи (FK)
- `userId` / `user_id` - ID пользователя (FK)
- `startTime` / `start_time` - Время начала (TIMESTAMP WITH TIMEZONE)
- `endTime` / `end_time` - Время окончания (nullable)
- `duration` - Длительность в секундах (INT)
- `description` - Описание работы (TEXT, nullable)
- `source` - Источник записи (enum: TIMER, MANUAL)
- `isActive` / `is_active` - Активен ли таймер (BOOLEAN)
- `deletedAt` / `deleted_at` - Время soft delete (nullable)
- `deleteReason` / `delete_reason` - Причина удаления (TEXT, nullable)

**Связанные термины**:
- **Timer** (Таймер) - активная запись времени (`isActive = true`)
- **Manual Time Entry** (Ручная запись) - запись с `source = MANUAL`
- **Soft Delete** (Мягкое удаление) - заполнение `deletedAt` вместо физического удаления
- **Overlapping Entry** (Пересекающаяся запись) - две записи с перекрывающимися временными интервалами

---

### TimeEntryAudit (Аудит записей времени)

**Backend**: `TimeEntryAudit`  
**Frontend**: `TimeEntryAudit`  
**Database**: `time_entry_audit`  
**API**: `/api/time-entries/{id}/audit`

**Атрибуты**:
- `auditId` / `audit_id` - Уникальный идентификатор
- `timeEntryId` / `time_entry_id` - ID записи времени (FK)
- `action` - Действие (enum: CREATED, EDITED, DELETED, RESTORED)
- `oldValue` / `old_value` - Старое значение (JSON, nullable)
- `newValue` / `new_value` - Новое значение (JSON, nullable)
- `reason` - Причина изменения (TEXT, required для EDIT/DELETE)
- `userId` / `user_id` - ID пользователя, выполнившего действие (FK)
- `timestamp` - Время действия (TIMESTAMP WITH TIMEZONE)

**Связанные термины**:
- **Audit Trail** (Журнал аудита) - полная история изменений записи
- **Restore** (Восстановление) - восстановление удалённой записи (ADMIN only)

---

### Comment (Комментарий)

**Backend**: `Comment`  
**Frontend**: `Comment`  
**Database**: `comments`  
**API**: `/api/tasks/{id}/comments`

**Атрибуты**:
- `commentId` / `comment_id` - Уникальный идентификатор
- `taskId` / `task_id` - ID задачи (FK)
- `userId` / `user_id` - ID автора (FK)
- `content` - Текст комментария (TEXT, markdown)
- `parentCommentId` / `parent_comment_id` - ID родительского комментария (nullable, для ответов)
- `createdAt` / `created_at` - Дата создания
- `updatedAt` / `updated_at` - Дата обновления (nullable)
- `isEdited` / `is_edited` - Флаг редактирования (BOOLEAN)

**Связанные термины**:
- **@mention** - упоминание пользователя в комментарии (e.g., `@john.doe`)
- **Nested Comment** (Вложенный комментарий) - ответ на комментарий (макс. 2 уровня)
- **Markdown** - формат текста для комментариев

---

### Tag (Метка/Тег)

**Backend**: `Tag`  
**Frontend**: `Tag`  
**Database**: `tags`  
**API**: `/api/tags`

**Атрибуты**:
- `tagId` / `tag_id` - Уникальный идентификатор
- `projectId` / `project_id` - ID проекта (FK, nullable - глобальные теги)
- `name` - Название тега (max 30 символов)
- `color` - Цвет тега (hex)
- `createdBy` / `created_by` - ID создателя (FK)

**Связанные термины**:
- **Global Tag** (Глобальный тег) - тег с `projectId = NULL`
- **Project Tag** (Проектный тег) - тег, специфичный для проекта
- **Task Tag** (Тег задачи) - связь many-to-many через `task_tags`

---

### File (Файл)

**Backend**: `File`  
**Frontend**: `File`  
**Database**: `files`  
**API**: `/api/files`

**Атрибуты**:
- `fileId` / `file_id` - Уникальный идентификатор
- `taskId` / `task_id` - ID задачи (FK)
- `uploadedBy` / `uploaded_by` - ID загрузившего (FK)
- `fileName` / `file_name` - Оригинальное имя файла
- `fileSize` / `file_size` - Размер файла в байтах (BIGINT)
- `mimeType` / `mime_type` - MIME тип (e.g., `application/pdf`)
- `storageKey` / `storage_key` - Ключ для хранилища (S3 или локальный путь)
- `uploadedAt` / `uploaded_at` - Дата загрузки

**Связанные термины**:
- **File Quota** (Квота файлов) - лимит хранилища (10 GB на проект)
- **File Size Limit** (Лимит размера) - макс. 10 MB на файл
- **Allowed MIME Types** (Разрешённые типы) - документы, изображения, архивы

---

### Notification (Уведомление)

**Backend**: `Notification`  
**Frontend**: `Notification`  
**Database**: `notifications`  
**API**: `/api/notifications`

**Атрибуты**:
- `notificationId` / `notification_id` - Уникальный идентификатор
- `userId` / `user_id` - ID получателя (FK)
- `type` - Тип (enum: TASK_ASSIGNED, MEMBER_ADDED, COMMENT_REPLY, DEADLINE_APPROACHING, etc.)
- `title` - Заголовок (max 100 символов)
- `message` - Текст уведомления
- `relatedEntityType` / `related_entity_type` - Тип сущности (enum: TASK, PROJECT, COMMENT)
- `relatedEntityId` / `related_entity_id` - ID сущности (UUID)
- `isRead` / `is_read` - Прочитано ли (BOOLEAN)
- `createdAt` / `created_at` - Дата создания

**Связанные термины**:
- **In-App Notification** (Внутреннее уведомление) - отображается в UI
- **Push Notification** (Push-уведомление) - браузерное уведомление
- **Notification Settings** (Настройки уведомлений) - предпочтения пользователя

---

## Technical Terms (Технические термины)

### Backend Architecture

- **Modular Monolith** (Модульный монолит) - архитектурный стиль с чёткими доменными границами
- **Reactive Programming** (Реактивное программирование) - неблокирующее выполнение (Mono, Flux)
- **R2DBC** - Reactive Relational Database Connectivity
- **Spring WebFlux** - реактивный фреймворк для backend
- **Constructor Injection** (Инъекция через конструктор) - DI паттерн (@RequiredArgsConstructor)

### Frontend Architecture

- **Smart Component** (Умный компонент) - контейнер с логикой и подпиской на store (Container)
- **Presentational Component** (Презентационный компонент) - Dumb-компонент с @Input/@Output
- **NgRx** - state management библиотека (Redux для Angular)
- **Facade Service** (Фасад-сервис) - прослойка между компонентами и store
- **OnPush Change Detection** - стратегия оптимизации рендеринга
- **Angular Signals** - реактивное состояние (для локального state)

### Database

- **Migration** (Миграция) - версионирование схемы БД (Flyway)
- **Soft Delete** (Мягкое удаление) - флаг `deleted_at` вместо физического удаления
- **Optimistic Locking** (Оптимистичная блокировка) - версионирование через `@Version`
- **Foreign Key** (Внешний ключ) - связь между таблицами (FK)
- **Index** (Индекс) - ускорение поиска по колонкам

### API & WebSocket

- **REST API** - RESTful HTTP API
- **WebSocket** - двунаправленная связь (STOMP protocol)
- **DTO** (Data Transfer Object) - объект для передачи данных между слоями
- **Mapper** (Маппер) - преобразование Entity ↔ DTO (MapStruct)
- **OpenAPI** - спецификация API (Swagger)

### Security

- **JWT** (JSON Web Token) - токен аутентификации
- **BCrypt** - алгоритм хеширования паролей
- **@PreAuthorize** - аннотация для проверки прав доступа
- **CORS** - Cross-Origin Resource Sharing
- **Rate Limiting** (Ограничение частоты) - защита от злоупотреблений

### Testing

- **Unit Test** (Модульный тест) - тест одного класса/компонента в изоляции
- **Integration Test** (Интеграционный тест) - тест взаимодействия компонентов
- **Contract Test** (Контрактный тест) - проверка соответствия API контракту
- **E2E Test** (End-to-End тест) - тест полного пользовательского сценария (Cypress)
- **Mock** - имитация зависимости (Mockito, Jasmine)
- **Testcontainers** - Docker-контейнеры для integration тестов

---

## Abbreviations (Сокращения)

| Сокращение | Полное название | Описание |
|------------|-----------------|----------|
| **API** | Application Programming Interface | Программный интерфейс |
| **DTO** | Data Transfer Object | Объект передачи данных |
| **FK** | Foreign Key | Внешний ключ |
| **PK** | Primary Key | Первичный ключ |
| **UUID** | Universally Unique Identifier | Универсальный уникальный идентификатор |
| **CRUD** | Create, Read, Update, Delete | Базовые операции с данными |
| **JWT** | JSON Web Token | Токен аутентификации |
| **SMTP** | Simple Mail Transfer Protocol | Протокол отправки email |
| **CORS** | Cross-Origin Resource Sharing | Кросс-доменные запросы |
| **TZ** | Timezone | Часовой пояс |
| **UTC** | Coordinated Universal Time | Всемирное координированное время |
| **SLF4J** | Simple Logging Facade for Java | Фасад для логирования |
| **HTTP** | HyperText Transfer Protocol | Протокол передачи гипертекста |
| **HTTPS** | HTTP Secure | Защищённый HTTP |
| **SQL** | Structured Query Language | Язык структурированных запросов |
| **JSON** | JavaScript Object Notation | Формат обмена данными |
| **STOMP** | Simple Text Oriented Messaging Protocol | Протокол обмена сообщениями |
| **MVC** | Model-View-Controller | Архитектурный паттерн |
| **DI** | Dependency Injection | Внедрение зависимостей |
| **MVP** | Minimum Viable Product | Минимально жизнеспособный продукт |
| **UI** | User Interface | Пользовательский интерфейс |
| **UX** | User Experience | Пользовательский опыт |

---

## Terminology Conflicts (Разрешение конфликтов)

### "Task" vs "Задача"
- **В коде (backend/frontend)**: всегда используется `Task`
- **В UI (русский интерфейс)**: всегда используется "задача"
- **В БД**: `tasks` (множественное число)

### "TimeEntry" vs "Time Entry" vs "Запись времени"
- **Backend class**: `TimeEntry` (одно слово, PascalCase)
- **Frontend interface**: `TimeEntry` (одно слово, PascalCase)
- **Database table**: `time_entries` (snake_case)
- **API endpoint**: `/time-entries` (kebab-case)
- **UI (русский)**: "запись времени"

### "ProjectMember" vs "Project Member"
- **Backend/Frontend**: `ProjectMember` (одно слово)
- **Database**: `project_members` (snake_case)
- **API**: `/projects/{id}/members` (вложенный ресурс)
- **UI**: "участник проекта"

### "Rich Text" vs "Markdown"
- **Task description**: используется Quill.js (rich text editor)
- **Comment content**: используется Markdown
- **Allowed HTML tags (Quill)**: `<p>`, `<strong>`, `<em>`, `<u>`, `<ol>`, `<ul>`, `<li>`, `<a>`, `<br>`

---

## Naming Patterns (Паттерны именования)

### Service Layer
- **Pattern**: `<Entity>Service` (e.g., `TaskService`, `ProjectService`)
- **Methods**: глагол + объект (e.g., `createTask`, `assignTaskToUser`, `getActiveTimer`)

### Repository Layer
- **Pattern**: `<Entity>Repository` (e.g., `TaskRepository`, `UserRepository`)
- **Methods**: стандартные Spring Data (e.g., `findById`, `save`, `deleteById`) + custom queries

### Controller Layer
- **Pattern**: `<Entity>Controller` (e.g., `TaskController`, `AuthController`)
- **Endpoints**: RESTful conventions (GET `/tasks`, POST `/tasks`, PUT `/tasks/{id}`)

### DTO Layer
- **Pattern**: `<Entity><Action>DTO` (e.g., `TaskCreateDTO`, `TaskResponseDTO`, `UserLoginDTO`)
- **Special cases**: `<Context>Request`/`Response` (e.g., `AddMemberRequest`, `LoginResponse`)

### Mapper Layer
- **Pattern**: `<Entity>Mapper` (e.g., `TaskMapper`, `ProjectMapper`)
- **Methods**: `toEntity`, `toDTO`, `toResponseDTO`

### Frontend Components
- **Smart**: `<Feature>ContainerComponent` (e.g., `TaskListContainerComponent`)
- **Dumb**: `<Feature>Component` (e.g., `TaskCardComponent`, `TaskFormComponent`)

### Frontend Services
- **API**: `<Feature>ApiService` (e.g., `TasksApiService`, `ProjectsApiService`)
- **Facade**: `<Feature>Facade` (e.g., `TasksFacade`, `ProjectsFacade`)

### Store (NgRx)
- **Actions**: `[<Feature>] <Action>` (e.g., `[Tasks] Load Tasks`, `[Auth] Login Success`)
- **Selectors**: `select<Feature><Property>` (e.g., `selectTasks`, `selectIsLoading`)
- **Effects**: `<action>$` (e.g., `loadTasks$`, `login$`)

---

## Status & Priority Enums

### TaskStatus
| Enum Value | Русский | Описание |
|------------|---------|----------|
| `NEW` | Новая | Задача создана, не начата |
| `IN_PROGRESS` | В работе | Задача в процессе выполнения |
| `TESTING` | Тестирование | Задача на проверке |
| `COMPLETED` | Завершена | Задача выполнена |
| `BLOCKED` | Заблокирована | Задача заблокирована внешними факторами |

### TaskPriority
| Enum Value | Русский | Описание |
|------------|---------|----------|
| `LOW` | Низкий | Несрочная задача |
| `MEDIUM` | Средний | Обычная задача |
| `HIGH` | Высокий | Важная задача |
| `URGENT` | Срочная | Критически важная задача |

### ProjectStatus
| Enum Value | Русский | Описание |
|------------|---------|----------|
| `ACTIVE` | Активный | Проект в работе |
| `ARCHIVED` | Архивный | Проект завершён (read-only) |

### UserRole (System)
| Enum Value | Русский | Описание |
|------------|---------|----------|
| `ADMIN` | Администратор | Полный доступ ко всей системе |
| `USER` | Пользователь | Обычный пользователь |

### ProjectMemberRole
| Enum Value | Русский | Описание |
|------------|---------|----------|
| `PROJECT_OWNER` | Владелец | Полный доступ к проекту |
| `PROJECT_MANAGER` | Менеджер | Управление задачами и участниками |
| `DEVELOPER` | Разработчик | Работа с задачами |
| `VIEWER` | Наблюдатель | Только просмотр (read-only) |

### NotificationType
| Enum Value | Русский | Описание |
|------------|---------|----------|
| `TASK_ASSIGNED` | Назначена задача | Пользователю назначена новая задача |
| `TASK_OVERDUE` | Задача просрочена | Дедлайн задачи истёк |
| `DEADLINE_APPROACHING` | Приближается дедлайн | До дедлайна осталось < 24ч |
| `MEMBER_ADDED` | Добавлен в проект | Пользователь добавлен в проект |
| `MEMBER_REMOVED` | Удалён из проекта | Пользователь удалён из проекта |
| `COMMENT_REPLY` | Ответ на комментарий | Новый ответ на комментарий |
| `MENTION` | Упоминание | Пользователь упомянут в комментарии |

---

## Best Practices (Лучшие практики)

### 1. Консистентность
- Всегда используйте одни и те же термины для одних и тех же концепций
- Не смешивайте `camelCase` и `snake_case` в одном контексте
- Следуйте конвенциям языка/фреймворка

### 2. Ясность
- Избегайте сокращений, кроме общепринятых (ID, DTO, API)
- Используйте полные имена: `billingRate` вместо `rate`, `projectMember` вместо `member`
- Префиксы для boolean: `is`, `has`, `should` (e.g., `isActive`, `hasAccess`)

### 3. Локализация
- Код всегда на английском (классы, переменные, методы)
- UI тексты на русском (labels, buttons, messages)
- Комментарии в коде: английский для технических деталей, русский для бизнес-логики (опционально)

### 4. Версионирование
- API endpoints: включайте версию `/api/v1/tasks` (если планируется эволюция API)
- Database migrations: последовательные номера `V1__initial_schema.sql`, `V2__add_comments.sql`

---

## References (Ссылки)

- [spec.md](./spec.md) - Полная спецификация требований
- [plan.md](./plan.md) - План реализации и техническая архитектура
- [tasks.md](./tasks.md) - Детальный список задач
- [data-model.md](./data-model.md) - Схема базы данных
- [openapi.yaml](./contracts/openapi.yaml) - API контракт
- [Constitution](../../.specify/memory/constitution.md) - Принципы проекта

---

**Дата последнего обновления**: 2025-11-16  
**Авторы**: AI Assistant (Cursor)  
**Статус**: ✅ Утверждён

