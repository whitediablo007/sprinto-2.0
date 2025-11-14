# Модель данных

**Проект**: Система управления проектами и задачами для малых команд  
**Дата**: 2025-11-14  
**Версия**: 1.0

## Обзор

Модель данных спроектирована для поддержки реактивного backend на R2DBC с PostgreSQL. Использует реляционную структуру с нормализацией для обеспечения целостности данных и эффективных запросов для отчетности.

---

## Диаграмма отношений (ER Diagram)

```
┌─────────────────────────────────────────────────────────────────┐
│                         Core Entities                            │
└─────────────────────────────────────────────────────────────────┘

users ──┬─── 1:N ──> projects (owner_id)
        ├─── N:M ──> projects (через project_members)
        ├─── 1:N ──> tasks (assignee_id, created_by)
        ├─── 1:N ──> time_entries
        ├─── 1:N ──> comments
        ├─── 1:N ──> notifications
        ├─── 1:1 ──> calendar_settings
        └─── 1:N ──> files (uploaded_by)

projects ──┬─── 1:N ──> tasks
           ├─── 1:N ──> tags (проектные)
           └─── N:M ──> users (через project_members)

tasks ──┬─── 1:N ──> tasks (parent_task_id - иерархия)
        ├─── 1:N ──> time_entries
        ├─── N:M ──> tags (через task_tags)
        ├─── 1:N ──> comments
        ├─── 1:N ──> files
        └─── N:M ──> tasks (через task_dependencies)

time_entries ─── 1:N ──> time_entry_audit (история изменений)
```

---

## Сущности и таблицы

### 1. Users (Пользователи)

**Назначение**: Представляет пользователей системы с их профилями и настройками.

**Таблица**: `users`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Email для аутентификации |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt хеш пароля |
| name | VARCHAR(255) | NOT NULL | Полное имя пользователя |
| avatar_url | VARCHAR(512) | NULL | URL аватара |
| hourly_rate | DECIMAL(10,2) | NULL | Ставка биллинга по умолчанию (руб/час) |
| is_admin | BOOLEAN | DEFAULT FALSE | Флаг SYSTEM_ADMIN роли |
| notification_preferences | JSONB | NOT NULL | Настройки уведомлений по каналам |
| do_not_disturb_until | TIMESTAMP | NULL | Режим "Не беспокоить" до указанного времени |
| timezone | VARCHAR(50) | DEFAULT 'Europe/Moscow' | Часовой пояс пользователя |
| created_at | TIMESTAMP | NOT NULL | Дата регистрации |
| updated_at | TIMESTAMP | NOT NULL | Дата последнего обновления профиля |

**Индексы**:
- `idx_users_email` (UNIQUE) на `email`
- `idx_users_is_admin` на `is_admin`

**Validation Rules**:
- Email: формат email (RFC 5322)
- Password: минимум 8 символов, включая цифры и буквы
- Name: минимум 2 символа
- Hourly_rate: >= 0

**JSONB структура notification_preferences**:
```json
{
  "taskAssigned": { "inApp": true, "email": true, "push": false },
  "statusChanged": { "inApp": true, "email": false, "push": false },
  "newComment": { "inApp": true, "email": true, "push": true },
  "deadlineApproaching": { "inApp": true, "email": true, "push": true },
  "deadlineExpired": { "inApp": true, "email": true, "push": true },
  "mentioned": { "inApp": true, "email": true, "push": true },
  "addedToProject": { "inApp": true, "email": true, "push": false }
}
```

---

### 2. Password Reset Tokens (Токены сброса пароля)

**Назначение**: Временные токены для процесса восстановления пароля.

**Таблица**: `password_reset_tokens`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| user_id | UUID | FK → users.id, NOT NULL | Пользователь |
| token | VARCHAR(255) | UNIQUE, NOT NULL | Токен восстановления (UUID) |
| expires_at | TIMESTAMP | NOT NULL | Время истечения токена (1 час) |
| used_at | TIMESTAMP | NULL | Время использования токена |
| created_at | TIMESTAMP | NOT NULL | Дата создания |

**Индексы**:
- `idx_password_reset_tokens_token` (UNIQUE) на `token`
- `idx_password_reset_tokens_user_id` на `user_id`

**Validation Rules**:
- Token действителен только если `used_at IS NULL` и `expires_at > NOW()`

---

### 3. Projects (Проекты)

**Назначение**: Контейнеры для задач и команд.

**Таблица**: `projects`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| owner_id | UUID | FK → users.id, NOT NULL | Владелец проекта |
| name | VARCHAR(255) | NOT NULL | Название проекта |
| description | TEXT | NULL | Описание (rich text в виде HTML) |
| color | VARCHAR(7) | NOT NULL | Цвет проекта (HEX формат #RRGGBB) |
| status | VARCHAR(20) | NOT NULL | ACTIVE, ARCHIVED, COMPLETED |
| hourly_rate | DECIMAL(10,2) | NULL | Ставка биллинга проекта (руб/час) |
| start_date | DATE | NULL | Дата начала проекта |
| end_date | DATE | NULL | Дата окончания проекта |
| created_at | TIMESTAMP | NOT NULL | Дата создания |
| updated_at | TIMESTAMP | NOT NULL | Дата последнего обновления |

**Индексы**:
- `idx_projects_owner_id` на `owner_id`
- `idx_projects_status` на `status`
- `idx_projects_owner_status` на `(owner_id, status)`

**Validation Rules**:
- Name: 1-255 символов
- Color: HEX формат #RRGGBB
- Status: один из [ACTIVE, ARCHIVED, COMPLETED]
- End_date >= start_date (если оба указаны)
- Hourly_rate: >= 0

---

### 4. Project Members (Участники проекта)

**Назначение**: Связь пользователей с проектами и их роли.

**Таблица**: `project_members`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| project_id | UUID | FK → projects.id, NOT NULL | Проект |
| user_id | UUID | FK → users.id, NOT NULL | Пользователь |
| role | VARCHAR(20) | NOT NULL | PROJECT_OWNER, PROJECT_MEMBER, PROJECT_OBSERVER |
| display_order | INTEGER | DEFAULT 0 | Порядок отображения (для drag & drop) |
| joined_at | TIMESTAMP | NOT NULL | Дата добавления в проект |

**Индексы**:
- `idx_project_members_project_id` на `project_id`
- `idx_project_members_user_id` на `user_id`
- `idx_project_members_unique` (UNIQUE) на `(project_id, user_id)`
- `idx_project_members_project_order` на `(project_id, display_order)`

**Validation Rules**:
- Role: один из [PROJECT_OWNER, PROJECT_MEMBER, PROJECT_OBSERVER]
- Комбинация (project_id, user_id) уникальна

---

### 5. Tasks (Задачи)

**Назначение**: Единицы работы в проектах.

**Таблица**: `tasks`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| project_id | UUID | FK → projects.id, NOT NULL | Проект |
| parent_task_id | UUID | FK → tasks.id, NULL | Родительская задача (для подзадач) |
| title | VARCHAR(500) | NOT NULL | Название задачи |
| description | TEXT | NULL | Описание (rich text в виде HTML) |
| assignee_id | UUID | FK → users.id, NULL | Исполнитель |
| created_by | UUID | FK → users.id, NOT NULL | Создатель задачи |
| status | VARCHAR(20) | NOT NULL | NEW, IN_PROGRESS, TESTING, BLOCKED, COMPLETED, CANCELLED |
| priority | VARCHAR(20) | NOT NULL | URGENT, HIGH, MEDIUM, LOW |
| deadline | TIMESTAMP | NULL | Дедлайн выполнения |
| estimated_hours | DECIMAL(8,2) | NULL | Оценка времени (часы) |
| hierarchy_level | INTEGER | DEFAULT 0 | Уровень вложенности (0-4, макс 5 уровней) |
| hierarchy_path | VARCHAR(255) | NULL | Путь в иерархии (для эффективных запросов) |
| completed_at | TIMESTAMP | NULL | Дата завершения |
| created_at | TIMESTAMP | NOT NULL | Дата создания |
| updated_at | TIMESTAMP | NOT NULL | Дата последнего обновления |

**Индексы**:
- `idx_tasks_project_id` на `project_id`
- `idx_tasks_assignee_id` на `assignee_id`
- `idx_tasks_parent_task_id` на `parent_task_id`
- `idx_tasks_status` на `status`
- `idx_tasks_priority` на `priority`
- `idx_tasks_deadline` на `deadline`
- `idx_tasks_project_status` на `(project_id, status)`
- `idx_tasks_assignee_status` на `(assignee_id, status)`
- `idx_tasks_hierarchy_path` (GIN) на `hierarchy_path` для эффективного поиска в дереве

**Validation Rules**:
- Title: 1-500 символов
- Status: один из [NEW, IN_PROGRESS, TESTING, BLOCKED, COMPLETED, CANCELLED]
- Priority: один из [URGENT, HIGH, MEDIUM, LOW]
- Hierarchy_level: 0-4 (макс 5 уровней)
- Estimated_hours: >= 0
- Parent_task_id: не может указывать на самого себя
- При создании подзадачи: hierarchy_level = parent.hierarchy_level + 1

**State Transitions**:
- NEW → IN_PROGRESS, CANCELLED
- IN_PROGRESS → TESTING, BLOCKED, COMPLETED, CANCELLED
- TESTING → IN_PROGRESS, COMPLETED, CANCELLED
- BLOCKED → IN_PROGRESS, CANCELLED
- COMPLETED → (финальный статус)
- CANCELLED → (финальный статус)

---

### 6. Task Dependencies (Зависимости задач)

**Назначение**: Представление зависимостей между задачами (задача B зависит от задачи A).

**Таблица**: `task_dependencies`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| dependent_task_id | UUID | FK → tasks.id, NOT NULL | Зависимая задача |
| blocking_task_id | UUID | FK → tasks.id, NOT NULL | Блокирующая задача |
| created_at | TIMESTAMP | NOT NULL | Дата создания зависимости |

**Индексы**:
- `idx_task_dependencies_dependent` на `dependent_task_id`
- `idx_task_dependencies_blocking` на `blocking_task_id`
- `idx_task_dependencies_unique` (UNIQUE) на `(dependent_task_id, blocking_task_id)`

**Validation Rules**:
- Dependent_task_id != blocking_task_id
- Нет циклических зависимостей (проверка при создании)

---

### 7. Tags (Метки)

**Назначение**: Категоризация задач через метки.

**Таблица**: `tags`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| name | VARCHAR(100) | NOT NULL | Название метки |
| color | VARCHAR(7) | NOT NULL | Цвет метки (HEX формат #RRGGBB) |
| project_id | UUID | FK → projects.id, NULL | Проект (NULL = глобальная метка) |
| created_at | TIMESTAMP | NOT NULL | Дата создания |

**Индексы**:
- `idx_tags_project_id` на `project_id`
- `idx_tags_name_project` (UNIQUE) на `(name, project_id)` - уникальность имени в рамках проекта

**Validation Rules**:
- Name: 1-100 символов
- Color: HEX формат #RRGGBB
- Комбинация (name, project_id) уникальна

---

### 8. Task Tags (Связь задач и меток)

**Назначение**: Many-to-Many связь между задачами и метками.

**Таблица**: `task_tags`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| task_id | UUID | FK → tasks.id, NOT NULL | Задача |
| tag_id | UUID | FK → tags.id, NOT NULL | Метка |
| assigned_at | TIMESTAMP | NOT NULL | Дата присвоения метки |

**Primary Key**: `(task_id, tag_id)`

**Индексы**:
- `idx_task_tags_task_id` на `task_id`
- `idx_task_tags_tag_id` на `tag_id`

---

### 9. Time Entries (Записи времени)

**Назначение**: Учет рабочего времени на задачах.

**Таблица**: `time_entries`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| task_id | UUID | FK → tasks.id, NOT NULL | Задача |
| user_id | UUID | FK → users.id, NOT NULL | Пользователь |
| start_time | TIMESTAMP | NOT NULL | Время начала работы |
| end_time | TIMESTAMP | NULL | Время окончания (NULL = активный таймер) |
| duration_seconds | INTEGER | NULL | Длительность в секундах (calculated) |
| hourly_rate | DECIMAL(10,2) | NULL | Ставка биллинга для этой записи |
| cost | DECIMAL(10,2) | NULL | Рассчитанная стоимость (calculated) |
| entry_type | VARCHAR(20) | NOT NULL | TIMER, MANUAL |
| description | TEXT | NULL | Комментарий к записи времени |
| deleted_at | TIMESTAMP | NULL | Soft delete timestamp |
| deleted_by | UUID | FK → users.id, NULL | Кто удалил запись |
| delete_reason | TEXT | NULL | Причина удаления |
| created_at | TIMESTAMP | NOT NULL | Дата создания |
| updated_at | TIMESTAMP | NOT NULL | Дата последнего обновления |

**Индексы**:
- `idx_time_entries_task_id` на `task_id`
- `idx_time_entries_user_id` на `user_id`
- `idx_time_entries_start_time` на `start_time`
- `idx_time_entries_user_time` на `(user_id, start_time)`
- `idx_time_entries_deleted_at` на `deleted_at` (для фильтрации удаленных)
- `idx_time_entries_active_timer` на `(user_id, end_time)` WHERE `end_time IS NULL` - для поиска активного таймера

**Validation Rules**:
- Entry_type: один из [TIMER, MANUAL]
- End_time > start_time (если не NULL)
- Duration_seconds >= 0
- Hourly_rate: >= 0
- Cost: >= 0
- У пользователя может быть только одна запись с end_time IS NULL (активный таймер)

**Calculated Fields**:
- `duration_seconds = EXTRACT(EPOCH FROM (end_time - start_time))` (при остановке таймера)
- `cost = (duration_seconds / 3600) * hourly_rate`

**Hourly Rate Priority** (заполняется при создании записи):
1. Task-specific rate (если есть в tasks.hourly_rate - будущее расширение)
2. Project rate (projects.hourly_rate)
3. User default rate (users.hourly_rate)

---

### 10. Time Entry Audit (Аудит изменений записей времени)

**Назначение**: История изменений для прозрачности и audit trail.

**Таблица**: `time_entry_audit`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| time_entry_id | UUID | FK → time_entries.id, NOT NULL | Запись времени |
| changed_by | UUID | FK → users.id, NOT NULL | Кто изменил |
| change_type | VARCHAR(20) | NOT NULL | CREATED, UPDATED, DELETED, RESTORED |
| change_reason | TEXT | NULL | Причина изменения |
| old_values | JSONB | NULL | Старые значения полей |
| new_values | JSONB | NULL | Новые значения полей |
| changed_at | TIMESTAMP | NOT NULL | Дата и время изменения |

**Индексы**:
- `idx_time_entry_audit_entry_id` на `time_entry_id`
- `idx_time_entry_audit_changed_by` на `changed_by`
- `idx_time_entry_audit_changed_at` на `changed_at`

**JSONB структура values**:
```json
{
  "start_time": "2025-11-14T10:00:00Z",
  "end_time": "2025-11-14T12:00:00Z",
  "duration_seconds": 7200,
  "hourly_rate": 1500.00,
  "cost": 3000.00
}
```

---

### 11. Comments (Комментарии)

**Назначение**: Комментарии к задачам для коммуникации в команде.

**Таблица**: `comments`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| task_id | UUID | FK → tasks.id, NOT NULL | Задача |
| user_id | UUID | FK → users.id, NOT NULL | Автор комментария |
| text | TEXT | NOT NULL | Текст комментария |
| mentioned_users | JSONB | NULL | Массив user_id упомянутых пользователей |
| created_at | TIMESTAMP | NOT NULL | Дата создания |
| updated_at | TIMESTAMP | NULL | Дата редактирования |

**Индексы**:
- `idx_comments_task_id` на `task_id`
- `idx_comments_user_id` на `user_id`
- `idx_comments_created_at` на `created_at`

**JSONB структура mentioned_users**:
```json
["uuid1", "uuid2", "uuid3"]
```

**Validation Rules**:
- Text: не пустой, макс 10000 символов

---

### 12. Notifications (Уведомления)

**Назначение**: In-app уведомления для пользователей.

**Таблица**: `notifications`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| user_id | UUID | FK → users.id, NOT NULL | Получатель |
| type | VARCHAR(50) | NOT NULL | TASK_ASSIGNED, STATUS_CHANGED, NEW_COMMENT, и т.д. |
| title | VARCHAR(255) | NOT NULL | Заголовок уведомления |
| message | TEXT | NOT NULL | Текст уведомления |
| related_entity_type | VARCHAR(50) | NULL | TASK, PROJECT, COMMENT |
| related_entity_id | UUID | NULL | ID связанной сущности |
| read_at | TIMESTAMP | NULL | Дата прочтения |
| created_at | TIMESTAMP | NOT NULL | Дата создания |

**Индексы**:
- `idx_notifications_user_id` на `user_id`
- `idx_notifications_read_at` на `read_at`
- `idx_notifications_user_unread` на `(user_id, read_at)` WHERE `read_at IS NULL`
- `idx_notifications_created_at` на `created_at`

**Notification Types**:
- TASK_ASSIGNED - назначена задача
- STATUS_CHANGED - изменен статус задачи
- NEW_COMMENT - новый комментарий
- DEADLINE_APPROACHING - приближается дедлайн (за 24 часа)
- DEADLINE_EXPIRED - просрочен дедлайн
- MENTIONED - упоминание в комментарии
- ADDED_TO_PROJECT - добавлен в проект
- PROJECT_UPDATED - изменение параметров проекта

**Validation Rules**:
- Type: один из предопределенных типов
- Title: 1-255 символов
- Message: не пустой

---

### 13. Files (Файлы)

**Назначение**: Прикрепленные к задачам файлы.

**Таблица**: `files`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| task_id | UUID | FK → tasks.id, NOT NULL | Задача |
| filename | VARCHAR(255) | NOT NULL | Оригинальное имя файла |
| stored_filename | VARCHAR(255) | NOT NULL | Имя файла в хранилище (UUID) |
| file_size | BIGINT | NOT NULL | Размер файла в байтах |
| mime_type | VARCHAR(100) | NOT NULL | MIME type |
| storage_path | VARCHAR(512) | NOT NULL | Путь к файлу в хранилище |
| uploaded_by | UUID | FK → users.id, NOT NULL | Кто загрузил |
| uploaded_at | TIMESTAMP | NOT NULL | Дата загрузки |

**Индексы**:
- `idx_files_task_id` на `task_id`
- `idx_files_uploaded_by` на `uploaded_by`

**Validation Rules**:
- File_size: 1 byte - 10 MB (10485760 bytes)
- MIME type: Whitelist разрешенных типов (изображения, документы, архивы)
- Filename: без опасных символов

**Supported MIME Types**:
- Images: image/jpeg, image/png, image/gif, image/webp
- Documents: application/pdf, application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document, application/vnd.ms-excel, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- Archives: application/zip, application/x-rar-compressed

---

### 14. Calendar Settings (Настройки Google Calendar)

**Назначение**: Конфигурация интеграции с Google Calendar для каждого пользователя.

**Таблица**: `calendar_settings`

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|----------|
| id | UUID | PK | Уникальный идентификатор |
| user_id | UUID | FK → users.id, UNIQUE, NOT NULL | Пользователь |
| google_calendar_id | VARCHAR(255) | NOT NULL | ID календаря Google |
| access_token_encrypted | TEXT | NOT NULL | Зашифрованный access token (AES-256) |
| refresh_token_encrypted | TEXT | NOT NULL | Зашифрованный refresh token (AES-256) |
| token_expires_at | TIMESTAMP | NOT NULL | Время истечения access token |
| sync_enabled | BOOLEAN | DEFAULT TRUE | Включена ли синхронизация |
| sync_filters | JSONB | NULL | Фильтры синхронизации |
| last_sync_at | TIMESTAMP | NULL | Дата последней синхронизации |
| created_at | TIMESTAMP | NOT NULL | Дата подключения |
| updated_at | TIMESTAMP | NOT NULL | Дата последнего обновления |

**Индексы**:
- `idx_calendar_settings_user_id` (UNIQUE) на `user_id`

**JSONB структура sync_filters**:
```json
{
  "priorities": ["URGENT", "HIGH"],
  "projectIds": ["uuid1", "uuid2"],
  "syncCompletedTasks": false
}
```

**Validation Rules**:
- Один пользователь может иметь только одну настройку календаря (user_id UNIQUE)

---

## Енумы (Enumerations)

### ProjectStatus
- `ACTIVE` - Активный проект
- `ARCHIVED` - Архивный проект (read-only)
- `COMPLETED` - Завершенный проект

### TaskStatus
- `NEW` - Новая задача
- `IN_PROGRESS` - В работе
- `TESTING` - На тестировании
- `BLOCKED` - Заблокирована
- `COMPLETED` - Завершена
- `CANCELLED` - Отменена

### TaskPriority
- `URGENT` - Срочно (красный)
- `HIGH` - Высокий (оранжевый)
- `MEDIUM` - Средний (желтый)
- `LOW` - Низкий (зеленый)

### ProjectMemberRole
- `PROJECT_OWNER` - Владелец проекта
- `PROJECT_MEMBER` - Участник проекта
- `PROJECT_OBSERVER` - Наблюдатель

### TimeEntryType
- `TIMER` - Создана через таймер
- `MANUAL` - Ручной ввод

### NotificationType
- `TASK_ASSIGNED` - Назначена задача
- `STATUS_CHANGED` - Изменен статус
- `NEW_COMMENT` - Новый комментарий
- `DEADLINE_APPROACHING` - Приближается дедлайн
- `DEADLINE_EXPIRED` - Просрочен дедлайн
- `MENTIONED` - Упоминание
- `ADDED_TO_PROJECT` - Добавлен в проект
- `PROJECT_UPDATED` - Обновлен проект

### AuditChangeType
- `CREATED` - Создание
- `UPDATED` - Обновление
- `DELETED` - Удаление
- `RESTORED` - Восстановление

---

## Миграции Flyway

### Нумерация версий

Формат: `V{version}__{description}.sql`

Примеры:
- `V1__initial_schema.sql`
- `V2__add_indexes.sql`
- `V3__add_calendar_settings.sql`

### V1: Initial Schema

```sql
-- Создание расширений
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Создание енумов
CREATE TYPE project_status AS ENUM ('ACTIVE', 'ARCHIVED', 'COMPLETED');
CREATE TYPE task_status AS ENUM ('NEW', 'IN_PROGRESS', 'TESTING', 'BLOCKED', 'COMPLETED', 'CANCELLED');
CREATE TYPE task_priority AS ENUM ('URGENT', 'HIGH', 'MEDIUM', 'LOW');
CREATE TYPE project_member_role AS ENUM ('PROJECT_OWNER', 'PROJECT_MEMBER', 'PROJECT_OBSERVER');
CREATE TYPE time_entry_type AS ENUM ('TIMER', 'MANUAL');
CREATE TYPE notification_type AS ENUM ('TASK_ASSIGNED', 'STATUS_CHANGED', 'NEW_COMMENT', 'DEADLINE_APPROACHING', 'DEADLINE_EXPIRED', 'MENTIONED', 'ADDED_TO_PROJECT', 'PROJECT_UPDATED');
CREATE TYPE audit_change_type AS ENUM ('CREATED', 'UPDATED', 'DELETED', 'RESTORED');

-- Создание таблиц (в порядке зависимостей)
-- ... (полный SQL будет в отдельном файле миграции)
```

### V2: Performance Indexes

```sql
-- Индексы для оптимизации запросов
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_assignee_status ON tasks(assignee_id, status);
CREATE INDEX idx_time_entries_user_time ON time_entries(user_id, start_time);
-- ... другие индексы
```

---

## Бизнес-правила и ограничения

### Иерархия задач
- Максимальная глубина вложенности: 5 уровней (0-4)
- При создании подзадачи: `hierarchy_level = parent.hierarchy_level + 1`
- `hierarchy_path` формируется как: `/parent_id/current_id/` для эффективных запросов поддерева

### Активный таймер
- Пользователь может иметь только один активный таймер (`time_entries.end_time IS NULL`)
- При запуске нового таймера проверяется отсутствие активного через уникальный partial index

### Биллинг
- Hourly rate применяется в приоритете: task > project > user
- Cost рассчитывается при остановке таймера или создании manual entry
- Cost и hourly_rate immutable после создания записи (для исторической корректности)

### Права доступа (проверяются на уровне сервиса)
- SYSTEM_ADMIN: доступ ко всем данным
- PROJECT_OWNER: CRUD в своих проектах
- PROJECT_MEMBER: READ проекта, CRUD своих задач, RW времени
- PROJECT_OBSERVER: READ-only доступ

### Soft Delete
- Time entries используют soft delete (`deleted_at NOT NULL`)
- Удаленные записи исключаются из отчетов через фильтр `WHERE deleted_at IS NULL`
- SYSTEM_ADMIN может восстанавливать записи

---

## Представления (Views) для оптимизации

### active_timers
```sql
CREATE VIEW active_timers AS
SELECT 
  te.id, te.user_id, te.task_id, te.start_time,
  EXTRACT(EPOCH FROM (NOW() - te.start_time)) as elapsed_seconds,
  t.title as task_title, p.name as project_name, p.color as project_color
FROM time_entries te
JOIN tasks t ON t.id = te.task_id
JOIN projects p ON p.id = t.project_id
WHERE te.end_time IS NULL;
```

### task_progress
```sql
CREATE VIEW task_progress AS
SELECT 
  t.id as task_id,
  COUNT(DISTINCT st.id) as total_subtasks,
  COUNT(DISTINCT st.id) FILTER (WHERE st.status IN ('COMPLETED', 'CANCELLED')) as completed_subtasks,
  CASE 
    WHEN COUNT(DISTINCT st.id) = 0 THEN 0
    ELSE ROUND((COUNT(DISTINCT st.id) FILTER (WHERE st.status IN ('COMPLETED', 'CANCELLED'))::numeric / COUNT(DISTINCT st.id)::numeric) * 100, 2)
  END as progress_percent
FROM tasks t
LEFT JOIN tasks st ON st.parent_task_id = t.id
GROUP BY t.id;
```

---

## Стратегия кэширования

### Часто читаемые, редко изменяемые данные:
- **users** (кэш профиля пользователя): TTL 15 минут
- **project_members** (права доступа): TTL 10 минут
- **tags**: TTL 30 минут

### Инвалидация:
- При изменении данных → publish event → invalidate cache key
- Spring Cache + Caffeine для in-memory кэша

---

## Резюме

- **Всего таблиц**: 14
- **Основные отношения**: 1:N, N:M через junction tables
- **Поддержка JSONB**: Для гибких настроек и метаданных
- **Индексы**: Оптимизированы для частых запросов (отчеты, списки задач)
- **Soft delete**: Для критичных данных (time_entries)
- **Audit trail**: Для записей времени
- **Нормализация**: 3NF для предотвращения аномалий

Модель данных спроектирована для эффективной работы с R2DBC в реактивном стеке и обеспечивает все функциональные требования из спецификации.

