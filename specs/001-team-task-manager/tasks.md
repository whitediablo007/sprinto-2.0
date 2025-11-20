# Задачи реализации: Система управления проектами и задачами

**Проект**: Team Task Manager  
**Ветка**: `001-team-task-manager`  
**Дата создания**: 2025-11-14  
**Спецификация**: [spec.md](./spec.md)  
**План реализации**: [plan.md](./plan.md)

---

## Обзор

Данный документ содержит детальный список задач для реализации системы управления проектами и задачами. Задачи организованы по пользовательским историям (User Stories) из спецификации и упорядочены по приоритетам P1 → P2 → P3.

**Всего задач**: 509  
**Приоритет P1**: 175 задач (US1, US2, Extended Features, Password Recovery, Audit Trail)  
**Приоритет P2**: 80 задач (US3, US4, US5)  
**Приоритет P3**: 87 задач (US6, US7, US8, US9)  
**Инфраструктурные и cross-cutting задачи (Phase 1, 2, 12 и др.)**: 167 задач

---

## Легенда

- `[ ]` - Задача не выполнена
- `[x]` - Задача выполнена
- **[P]** - Задача может выполняться параллельно с другими
- **[US#]** - Принадлежность к пользовательской истории
- **T###** - Уникальный идентификатор задачи

---

## Стратегия реализации

### MVP Scope (Минимально жизнеспособный продукт)

**Рекомендуется начать с User Story 1 (US1)** - это полностью функционирующий MVP, который доставляет ценность:
- Создание задач
- Учет времени через таймеры
- Базовое управление статусами

После успешной реализации US1 можно добавлять остальные функции инкрементально.

### Порядок выполнения

1. **Phase 1**: Setup — инициализация backend/frontend проектов и инфраструктуры БД
2. **Phase 2**: Foundational — базовая инфраструктура (безопасность, WebSocket, Auth, NgRx, миграции) и тестовый каркас
3. **Phase 3**: User Story 1 — базовое управление задачами (P1) + таймеры и учёт времени
4. **Phase 4**: User Story 2 — управление проектами и командой (P1)
5. **Phase 4.5**: Extended Features — комментарии, метки, файловые вложения (P1)
6. **Phase 5-7**: User Stories 3-5 (P2) — дашборд, отчёты, система уведомлений
7. **Phase 8-11**: User Stories 6-9 (P3) — интеграции, BI-аналитика, рейтинг, иерархия задач
8. **Phase 12**: Polish & Testing — cross-cutting задачи, покрытие тестами, производительность, аналитика, CI/CD

### Параллельное выполнение

Задачи с маркером **[P]** могут выполняться параллельно, если:
- Работают с разными файлами
- Не имеют зависимостей от незавершенных задач

**Пример**: В Phase 3 (US1) задачи по backend (T026-T045) и frontend (T046-T068) могут выполняться параллельно разными разработчиками.

---

## Phase 1: Setup (Инициализация проектов)

**Цель**: Создать базовую структуру backend и frontend проектов с настройкой инструментов сборки.

**Критерии завершения**:
- ✅ Backend проект успешно собирается через Gradle
- ✅ Frontend проект успешно собирается через npm/Angular CLI
- ✅ PostgreSQL запускается через Docker Compose
- ✅ Приложения успешно стартуют локально

### Задачи

- [x] T001 Создать backend проект: Spring Boot с Gradle Kotlin DSL в директории backend/
- [x] T002 [P] Создать frontend проект: Angular 18+ с Tailwind CSS в директории frontend/
- [x] T003 [P] Настроить Docker Compose с PostgreSQL 15+ в корне репозитория
- [x] T004 [P] Настроить EditorConfig и Git hooks для форматирования кода (.editorconfig в корне)
- [x] T005 Создать базовую структуру директорий backend согласно plan.md
- [x] T006 [P] Создать базовую структуру директорий frontend согласно plan.md (feature modules)

---

## Phase 2: Foundational Infrastructure (Базовая инфраструктура)

**Цель**: Реализовать фундаментальную инфраструктуру, необходимую для всех user stories.

**Критерии завершения**:
- ✅ База данных создается автоматически через Flyway миграции
- ✅ JWT аутентификация работает (регистрация, логин, refresh token)
- ✅ WebSocket соединение устанавливается и передает сообщения
- ✅ Angular Guards защищают маршруты
- ✅ HTTP Interceptors добавляют JWT токены к запросам
- ✅ NgRx Store инициализирована и DevTools работают

### Backend Infrastructure

- [x] T007 Создать flyway миграцию V1__initial_schema.sql с енумами и таблицей users в backend/src/main/resources/db/migration/
- [x] T008 Создать flyway миграцию V2__projects_and_members.sql с таблицами projects, project_members
- [x] T009 Создать flyway миграцию V3__tasks_and_dependencies.sql с таблицами tasks, task_dependencies, tags, task_tags
- [x] T010 Создать flyway миграцию V4__time_tracking.sql с таблицами time_entries, time_entry_audit
- [x] T011 Создать flyway миграцию V5__comments_and_notifications.sql с таблицами comments, notifications, files
- [x] T012 Создать flyway миграцию V6__calendar_and_indexes.sql с таблицей calendar_settings и performance индексами
- [x] T013 [P] Настроить R2DBC connection pool в backend/src/main/resources/application.yml (PostgreSQL R2DBC driver)
- [x] T014 [P] Настроить Spring Security для WebFlux в backend/src/main/java/ru/get/tms/config/SecurityConfig.java
- [x] T015 Создать JWT utility класс в backend/src/main/java/ru/get/tms/security/JwtUtil.java (генерация, валидация токенов)
- [x] T016 [P] Создать domain entity User в backend/src/main/java/ru/get/tms/domain/user/User.java
- [x] T017 [P] Создать UserRepository (R2DBC) в backend/src/main/java/ru/get/tms/repository/UserRepository.java
- [x] T018 Реализовать AuthService с регистрацией и логином в backend/src/main/java/ru/get/tms/service/AuthService.java
- [x] T019 Реализовать AuthController (POST /api/auth/register, /api/auth/login, /api/auth/refresh) в backend/src/main/java/ru/get/tms/api/rest/AuthController.java

### Password Recovery (FR-007.1)

- [x] T537 [P] Создать PasswordResetToken entity в backend/src/main/java/ru/get/tms/domain/user/PasswordResetToken.java (token, userId, expiresAt)
- [x] T538 [P] Создать PasswordResetTokenRepository (R2DBC) в backend/src/main/java/ru/get/tms/repository/PasswordResetTokenRepository.java
- [x] T539 Реализовать EmailService с SMTP для отправки писем в backend/src/main/java/ru/get/tms/service/EmailService.java
- [x] T540 Реализовать PasswordResetService (generateToken, validateToken, resetPassword) в backend/src/main/java/ru/get/tms/service/PasswordResetService.java
- [x] T541 Добавить endpoints в AuthController: POST /api/auth/password-reset/request, POST /api/auth/password-reset/confirm
- [x] T542 [P] Создать ForgotPasswordComponent (Dumb) с email input в frontend/src/app/features/auth/components/forgot-password.component.ts
- [x] T543 [P] Создать ResetPasswordComponent (Dumb) с token validation в frontend/src/app/features/auth/components/reset-password.component.ts
- [x] T544 Написать integration тест для password reset flow в backend/src/test/java/ru/get/tms/integration/PasswordResetFlowTest.java

### Security: Password Policy & Session Inactivity (FR-007.2, FR-007.3, NFR-020, SC-001)

- [x] T700 [P] Реализовать валидацию сложности пароля на backend (Bean Validation аннотации для полей регистрации/смены пароля согласно FR-007.2) и обновить сообщения об ошибках (см. FR-007.2, NFR-021)
- [x] T701 [P] Добавить валидацию сложности пароля и отображение ошибок на формах регистрации/смены пароля на frontend (Reactive Forms + общая карта сообщений валидации) (см. FR-007.2, SC-001)
- [x] T702 Задокументировать и реализовать политику истечения пользовательской сессии по неактивности (по умолчанию 30 минут, конфиг через переменные окружения) в механизме access/refresh токенов (см. FR-007.3, NFR-020)
- [x] T703 [P] Реализовать на frontend idle‑таймер, который отслеживает активность пользователя (клики/нажатия клавиш/запросы) и инициирует logout при достижении порога неактивности, согласованного с FR-007.3 (см. FR-007.3, SC-001)
- [x] T704 Написать integration/E2E тесты, подтверждающие авто‑выход пользователя после периода неактивности и корректную работу повторной аутентификации (см. FR-007.2, FR-007.3, SC-001)

- [x] T020 [P] Настроить WebSocket endpoint с STOMP в backend/src/main/java/ru/get/tms/config/WebSocketConfig.java
- [x] T021 [P] Создать GlobalExceptionHandler с @ControllerAdvice в backend/src/main/java/ru/get/tms/exception/GlobalExceptionHandler.java
- [x] T022 [P] Настроить Logback для INFO/DEBUG уровней в backend/src/main/resources/logback-spring.xml

### Backend: Reactive Error Handling Pattern (NFR-027)

- [x] T660 Спроектировать единый паттерн реактивной обработки ошибок для WebFlux-сервисов (onErrorResume/onErrorMap, доменные исключения, маппинг в ErrorResponse) и задокументировать его в разделе Exception Handling backend-архитектуры
- [x] T661 [P] Применить паттерн реактивной обработки ошибок в ключевых сервисах (AuthService, PasswordResetService), заменив ad-hoc обработку на стандартизированный подход
- [x] T662 [P] Написать unit и integration тесты для проверки реактивного error-handling (корректный маппинг ошибок в ErrorResponse, отсутствие обрыва потоков) в backend/src/test/java/ru/get/tms/unit/ReactiveErrorHandlerTest.java и integration/ReactiveErrorHandlingIntegrationTest.java
- [x] T663 [P] Обновить чеклист code review пунктом о проверке использования onErrorResume/onErrorMap и соблюдении NFR-027/конституции при реализации WebFlux-сервисов в backend/CODE_REVIEW_CHECKLIST.md

### Backend: Structured Logging & Correlation ID

- [ ] T640 [P] Настроить JSON‑формат логов в Logback для production‑профиля (structured logging) согласно требованиям NFR-038–NFR-043 в backend/src/main/resources/logback-spring.xml
- [ ] T641 Реализовать WebFlux/WebFilter, генерирующий Correlation ID (если не передан) и записывающий его в MDC для всех входящих HTTP/WebSocket запросов
- [ ] T642 Обновить GlobalExceptionHandler для логирования ошибок с Correlation ID и возврата его в ответе (заголовок/поле ответа)
- [ ] T643 [P] Написать integration тест, проверяющий наличие JSON‑логов с Correlation ID для типового REST‑запроса в backend/src/test/java/com/sprinto/tms/integration/StructuredLoggingTest.java

### Frontend Infrastructure

- [ ] T023 Настроить Angular environment files в frontend/src/environments/ (API URLs, WebSocket URL)
- [ ] T024 Создать HTTP Interceptor для JWT токенов в frontend/src/app/core/interceptors/auth.interceptor.ts
- [ ] T025 Создать Auth Guard для защиты маршрутов в frontend/src/app/core/guards/auth.guard.ts
- [ ] T026 [P] Настроить NgRx root store в frontend/src/app/store/root-state.ts и root-reducers.ts
- [ ] T027 [P] Настроить NgRx DevTools в frontend/src/app/app.module.ts
- [ ] T028 [P] Создать WebSocket service с RxStomp в frontend/src/app/core/services/websocket.service.ts
- [ ] T629 [P] Реализовать auto-reconnect в WebSocketService с экспоненциальным backoff и ограничением максимальной задержки между попытками
- [ ] T630 [P] Реализовать повторную подписку на ключевые каналы (/user/queue/timer, /user/queue/notifications и др.) после переподключения и проброс статуса соединения в UI
- [ ] T029 [P] Создать AuthService с методами login/register/logout в frontend/src/app/core/auth/auth.service.ts
- [ ] T030 Создать auth feature module со store (actions, reducer, effects, selectors) в frontend/src/app/features/auth/
- [ ] T031 [P] Настроить Tailwind CSS в frontend/tailwind.config.js и frontend/src/styles/tailwind.css
- [ ] T032 [P] Настроить PrimeNG theme в frontend/src/styles/primeng-theme.scss

### Testing Infrastructure

- [ ] T033 [P] Написать unit тест для AuthService в backend/src/test/java/com/sprinto/tms/unit/AuthServiceTest.java
- [ ] T034 [P] Написать integration тест для AuthController в backend/src/test/java/com/sprinto/tms/integration/AuthControllerTest.java
- [ ] T035 [P] Написать unit тест для JwtUtil в backend/src/test/java/com/sprinto/tms/unit/JwtUtilTest.java
- [ ] T036 [P] Написать WebSocket integration тест в backend/src/test/java/com/sprinto/tms/integration/WebSocketTest.java
- [ ] T037 [P] Написать unit тест для AuthService (frontend) в frontend/src/app/core/auth/auth.service.spec.ts
- [ ] T038 [P] Написать unit тест для auth store reducer в frontend/src/app/features/auth/store/auth.reducer.spec.ts
- [ ] T039 [P] Написать unit тест для auth store effects в frontend/src/app/features/auth/store/auth.effects.spec.ts
- [ ] T040 [P] Написать unit тест для WebSocketService в frontend/src/app/core/services/websocket.service.spec.ts

---

## Phase 3: User Story 1 - Базовое управление задачами (P1)

**Цель**: Реализовать MVP - создание задач, запуск/остановка таймера, изменение статуса.

**Обоснование приоритета**: Это основа системы. Без возможности создавать задачи и учитывать время работы остальной функционал теряет смысл.

**Критерии завершения (Acceptance Criteria)**:
- ✅ Пользователь может создать задачу с названием и описанием
- ✅ Задача сохраняется со статусом NEW и отображается в списке
- ✅ Пользователь может запустить таймер на задаче
- ✅ Таймер отображается как floating button и обновляется в реальном времени (WebSocket)
- ✅ При остановке таймера время сохраняется как запись времени
- ✅ Пользователь может изменить статус задачи на COMPLETED
- ✅ При попытке запустить второй таймер система показывает уведомление

### Backend: Domain & Repository

- [ ] T041 [P] [US1] Создать domain entity Project в backend/src/main/java/com/sprinto/tms/domain/project/Project.java
- [ ] T042 [P] [US1] Создать domain entity Task в backend/src/main/java/com/sprinto/tms/domain/task/Task.java
- [ ] T043 [P] [US1] Создать domain entity TimeEntry в backend/src/main/java/com/sprinto/tms/domain/timeentry/TimeEntry.java
- [ ] T044 [P] [US1] Создать ProjectRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/ProjectRepository.java
- [ ] T045 [P] [US1] Создать TaskRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/TaskRepository.java
- [ ] T046 [P] [US1] Создать TimeEntryRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/TimeEntryRepository.java

### Backend: DTOs & Mappers

- [ ] T047 [P] [US1] Создать TaskCreateDTO и TaskResponseDTO в backend/src/main/java/com/sprinto/tms/dto/task/
- [ ] T048 [P] [US1] Создать TimeEntryDTO и TimerUpdateDTO в backend/src/main/java/com/sprinto/tms/dto/timeentry/
- [ ] T049 [P] [US1] Создать TaskMapper (MapStruct) в backend/src/main/java/com/sprinto/tms/mapper/TaskMapper.java
- [ ] T050 [P] [US1] Создать TimeEntryMapper (MapStruct) в backend/src/main/java/com/sprinto/tms/mapper/TimeEntryMapper.java

### Backend: Services

- [ ] T051 [US1] Реализовать TaskService с методами create, findById, updateStatus в backend/src/main/java/com/sprinto/tms/service/TaskService.java
- [ ] T052 [US1] Реализовать TimeTrackingService с методами startTimer, stopTimer, getActiveTimer в backend/src/main/java/com/sprinto/tms/service/TimeTrackingService.java
- [ ] T053 [US1] Реализовать TimerScheduler для отправки WebSocket обновлений каждую секунду в backend/src/main/java/com/sprinto/tms/service/TimerScheduler.java

### Backend: Controllers & WebSocket

- [ ] T054 [US1] Реализовать TaskController (POST /api/tasks, GET /api/tasks/{id}, PUT /api/tasks/{id}/status) в backend/src/main/java/com/sprinto/tms/api/rest/TaskController.java
- [ ] T055 [US1] Реализовать TimeTrackingController (POST /api/time-entries/timer/start, POST /api/time-entries/timer/stop) в backend/src/main/java/com/sprinto/tms/api/rest/TimeTrackingController.java
- [ ] T056 [US1] Создать WebSocket handler для отправки timer updates на /user/queue/timer в backend/src/main/java/com/sprinto/tms/api/websocket/TimerWebSocketHandler.java

### Time Entry Audit Trail (FR-056-059)

- [ ] T545 [P] Создать TimeEntryAudit entity в backend/src/main/java/com/sprinto/tms/domain/timeentry/TimeEntryAudit.java (auditId, timeEntryId, action, oldValue, newValue, reason, userId, timestamp)
- [ ] T546 [P] Создать TimeEntryAuditRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/TimeEntryAuditRepository.java
- [ ] T547 Реализовать AuditTrailService для автоматического логирования изменений TimeEntry в backend/src/main/java/com/sprinto/tms/service/AuditTrailService.java
- [ ] T548 Интегрировать AuditTrailService в TimeTrackingService (логирование EDIT, DELETE, RESTORE операций)
- [ ] T549 Добавить endpoint GET /api/time-entries/{id}/audit в TimeTrackingController для получения истории изменений
- [ ] T550 [P] Создать TimeEntryAuditDTO в backend/src/main/java/com/sprinto/tms/dto/timeentry/TimeEntryAuditDTO.java
- [ ] T551 [P] Создать TimeEntryAuditListComponent (Dumb) для отображения истории изменений в frontend/src/app/features/time-tracking/components/time-entry-audit-list.component.ts
- [ ] T552 [P] Создать RestoreTimeEntryDialogComponent (Dumb) для восстановления удалённых записей (ADMIN) в frontend/src/app/features/time-tracking/components/restore-time-entry-dialog.component.ts
- [ ] T553 Написать integration тест для audit trail в backend/src/test/java/com/sprinto/tms/integration/TimeEntryAuditTest.java

### Timer State Persistence (FR-063, NFR-045, SC-010)

- [ ] T600 [US1] Реализовать сохранение состояния активного таймера на backend (расширение модели TimeEntry или отдельный snapshot-entity) с привязкой к пользователю и задаче
- [ ] T601 [US1] Добавить REST endpoint или метод в TimeTrackingController для получения текущего активного таймера пользователя при входе в систему
- [ ] T602 [US1] Обновить TimerFacade и TimerContainerComponent во frontend для запроса состояния таймера при инициализации и показа диалога «продолжить/остановить» при обнаружении активного таймера
- [ ] T603 [P] [US1] Написать integration тест для сценария восстановления активного таймера (закрытие браузера → повторный вход) в backend/src/test/java/com/sprinto/tms/integration/ActiveTimerPersistenceTest.java
- [ ] T604 [P] [US1] Написать E2E тест для SC-010 (сохранение/восстановление активного таймера) в frontend/tests/e2e/active-timer-persistence.spec.ts

### Time Entry Overlap Conflict Visualization (FR-054, FR-064, SC-016)

- [ ] T714 [US1] Добавить на backend API для получения списка пересекающихся записей времени пользователя для указанного интервала и задачи (FR-054, FR-064, SC-016)
- [ ] T715 [US1] Реализовать на frontend диалог визуализации конфликта пересечения записей времени (timeline/график) при добавлении/редактировании записи, включая явные варианты подтверждения/отмены (см. FR-054, FR-064, SC-016)
- [ ] T716 [P] [US1] Написать integration и E2E тесты, проверяющие корректное обнаружение пересечений, отображение предупреждения и поведение при подтверждении/отмене конфликта (см. FR-054, FR-064, SC-016)

### Frontend: Models & Store

- [ ] T057 [P] [US1] Создать Task model interface в frontend/src/app/shared/models/task.model.ts
- [ ] T058 [P] [US1] Создать TimeEntry model interface в frontend/src/app/shared/models/time-entry.model.ts
- [ ] T059 [US1] Создать tasks feature store (actions, reducer, effects, selectors) в frontend/src/app/features/tasks/store/
- [ ] T060 [US1] Создать time-tracking feature store (actions, reducer, effects, selectors) в frontend/src/app/features/time-tracking/store/

### Frontend: Services

- [ ] T061 [P] [US1] Создать TasksApiService с методами create, getById, updateStatus в frontend/src/app/features/tasks/services/tasks-api.service.ts
- [ ] T062 [P] [US1] Создать TimeTrackingApiService с методами startTimer, stopTimer в frontend/src/app/features/time-tracking/services/time-tracking-api.service.ts
- [ ] T063 [US1] Создать TasksFacade service для скрытия NgRx details в frontend/src/app/features/tasks/services/tasks-facade.service.ts
- [ ] T064 [US1] Создать TimerFacade service в frontend/src/app/features/time-tracking/services/timer-facade.service.ts

### Frontend: Components (Presentational)

- [ ] T065 [P] [US1] Создать TaskCardComponent (Dumb) с @Input task, @Output edit/delete в frontend/src/app/features/tasks/components/task-card.component.ts
- [ ] T066 [P] [US1] Создать TaskFormComponent (Dumb) с Reactive Form в frontend/src/app/features/tasks/components/task-form.component.ts
- [ ] T067 [P] [US1] Создать TimerDisplayComponent (Dumb) для floating button таймера в frontend/src/app/features/time-tracking/components/timer-display.component.ts
- [ ] T068 [P] [US1] Создать TaskStatusBadgeComponent (Dumb) для отображения статуса в frontend/src/app/shared/components/task-status-badge.component.ts

### Frontend: Components (Smart/Container)

- [ ] T069 [US1] Создать TaskListContainerComponent (Smart) с подпиской на store в frontend/src/app/features/tasks/containers/task-list-container.component.ts
- [ ] T070 [US1] Создать TaskDetailsContainerComponent (Smart) в frontend/src/app/features/tasks/containers/task-details-container.component.ts
- [ ] T071 [US1] Создать TimerContainerComponent (Smart) с WebSocket подпиской на /user/queue/timer в frontend/src/app/features/time-tracking/containers/timer-container.component.ts

### Frontend: Routing & Module

- [ ] T072 [US1] Настроить tasks routing module с lazy loading в frontend/src/app/features/tasks/tasks-routing.module.ts
- [ ] T073 [US1] Создать TasksModule с импортом PrimeNG компонентов в frontend/src/app/features/tasks/tasks.module.ts
- [ ] T074 [US1] Настроить app-routing для /tasks маршрута в frontend/src/app/app-routing.module.ts

### Integration & Testing

- [ ] T075 [US1] Написать integration тест для TaskController в backend/src/test/java/com/sprinto/tms/integration/TaskControllerTest.java
- [ ] T076 [US1] Написать integration тест для TimeTrackingController в backend/src/test/java/com/sprinto/tms/integration/TimeTrackingControllerTest.java
- [ ] T077 [US1] Написать unit тест для TaskService в backend/src/test/java/com/sprinto/tms/unit/TaskServiceTest.java
- [ ] T078 [US1] Написать unit тест для TimeTrackingService в backend/src/test/java/com/sprinto/tms/unit/TimeTrackingServiceTest.java
- [ ] T079 [US1] Написать unit тест для TimerScheduler в backend/src/test/java/com/sprinto/tms/unit/TimerSchedulerTest.java
- [ ] T080 [P] [US1] Написать unit тест для TaskFormComponent в frontend/src/app/features/tasks/components/task-form.component.spec.ts
- [ ] T081 [P] [US1] Написать unit тест для TaskCardComponent в frontend/src/app/features/tasks/components/task-card.component.spec.ts
- [ ] T082 [P] [US1] Написать unit тест для tasks store reducer в frontend/src/app/features/tasks/store/tasks.reducer.spec.ts
- [ ] T083 [P] [US1] Написать unit тест для tasks store effects в frontend/src/app/features/tasks/store/tasks.effects.spec.ts
- [ ] T084 [P] [US1] Написать unit тест для time-tracking store reducer в frontend/src/app/features/time-tracking/store/time-tracking.reducer.spec.ts
- [ ] T085 [P] [US1] Написать unit тест для TasksApiService в frontend/src/app/features/tasks/services/tasks-api.service.spec.ts

---

## Phase 4: User Story 2 - Управление проектами и командой (P1)

**Цель**: Реализовать создание проектов, управление участниками, настройки проекта.

**Обоснование приоритета**: Без возможности создавать проекты и управлять командой система не может функционировать для командной работы.

**Критерии завершения**:
- ✅ PROJECT_OWNER может создать проект с полями: название, описание, цвет, даты, биллинг
- ✅ Владелец может добавлять участников по email с назначением ролей
- ✅ Участники видят проект в своем списке и получают уведомление
- ✅ Владелец может назначать задачи участникам
- ✅ Владелец может удалять участников с подтверждением
- ✅ Изменения настроек проекта сохраняются

### Backend: Domain & Repository

- [ ] T554 [P] [US2] Создать domain entity ProjectMember в backend/src/main/java/com/sprinto/tms/domain/project/ProjectMember.java
- [ ] T555 [P] [US2] Создать ProjectMemberRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/ProjectMemberRepository.java

### Backend: DTOs & Services

- [ ] T556 [P] [US2] Создать ProjectCreateDTO, ProjectUpdateDTO, ProjectResponseDTO в backend/src/main/java/com/sprinto/tms/dto/project/
- [ ] T557 [P] [US2] Создать ProjectMemberDTO, AddMemberRequest в backend/src/main/java/com/sprinto/tms/dto/project/
- [ ] T558 [P] [US2] Создать ProjectMapper (MapStruct) в backend/src/main/java/com/sprinto/tms/mapper/ProjectMapper.java
- [ ] T559 [US2] Реализовать ProjectService с методами create, update, addMember, removeMember, getMembers в backend/src/main/java/com/sprinto/tms/service/ProjectService.java
- [ ] T560 [US2] Реализовать ProjectMemberService для управления ролями в backend/src/main/java/com/sprinto/tms/service/ProjectMemberService.java

### Backend: Project Archive & Restoration (FR-015–FR-019, NFR-045, NFR-047)

- [ ] T705 [US2] Расширить ProjectService методами archiveProject, restoreProject, deleteProjectHard, обеспечив soft delete/архивацию проектов, остановку активных таймеров и соблюдение бизнес-логики FR-015–FR-019 (см. FR-015, FR-017, FR-018, FR-019, NFR-045)
- [ ] T706 [US2] Создать @Scheduled‑задачу для периодического удаления проектов, находящихся в архиве более 30 дней, с логированием и учётом NFR-047 по надёжности (см. FR-017, NFR-047)

### Backend: Controllers

- [ ] T561 [US2] Реализовать ProjectController (POST, GET, PUT, DELETE /api/projects) в backend/src/main/java/com/sprinto/tms/api/rest/ProjectController.java
- [ ] T562 [US2] Реализовать ProjectMembersController (POST /api/projects/{id}/members, DELETE /api/projects/{id}/members/{userId}) в backend/src/main/java/com/sprinto/tms/api/rest/ProjectMembersController.java

### Frontend: Project Archive UI (FR-015–FR-019)

- [ ] T707 [US2] Добавить в ProjectDetailsContainerComponent и ProjectSettingsContainerComponent действия архивирования/восстановления/окончательного удаления проекта с подтверждающими диалогами и предупреждениями согласно FR-015–FR-019 (см. FR-015, FR-016, FR-017, FR-018)
- [ ] T708 [US2] Реализовать отображение списка архивных проектов и фильтров по статусам (Активный/Архивный/Завершенный) в ProjectListContainerComponent (см. FR-009, FR-015–FR-018)
- [ ] T709 [US2] Обновить ProjectsApiService и ProjectsFacade для поддержки новых операций архивации/восстановления и обновления локального состояния (см. FR-015–FR-018)

### Testing: Project Archive Lifecycle (FR-015–FR-019, NFR-045, NFR-047)

- [ ] T710 [US2] Написать integration тесты для полного жизненного цикла проекта (create → archive → restore → hard delete после 30+ дней) в backend/src/test/java/com/sprinto/tms/integration/ProjectArchiveLifecycleTest.java (см. FR-015–FR-019, NFR-045, NFR-047)

### Backend: Security & Authorization

- [ ] T563 [US2] Создать ProjectPermissionEvaluator для проверки прав доступа в backend/src/main/java/com/sprinto/tms/security/ProjectPermissionEvaluator.java
- [ ] T564 [US2] Добавить @PreAuthorize аннотации в ProjectController с проверкой PROJECT_OWNER роли

### Backend: WebSocket Events

- [ ] T565 [US2] Создать ProjectEventPublisher для отправки событий на /topic/project.{projectId} в backend/src/main/java/com/sprinto/tms/api/websocket/ProjectEventPublisher.java
- [ ] T566 [US2] Интегрировать ProjectEventPublisher в ProjectService (события MEMBER_ADDED, MEMBER_REMOVED, PROJECT_UPDATED)

### Frontend: Models & Store

- [ ] T567 [P] [US2] Создать Project model interface в frontend/src/app/shared/models/project.model.ts
- [ ] T086 [P] [US2] Создать ProjectMember model interface в frontend/src/app/shared/models/project-member.model.ts
- [ ] T087 [US2] Создать projects feature store (actions, reducer, effects, selectors) в frontend/src/app/features/projects/store/

### Frontend: Services

- [ ] T088 [P] [US2] Создать ProjectsApiService с CRUD методами в frontend/src/app/features/projects/services/projects-api.service.ts
- [ ] T089 [US2] Создать ProjectsFacade service в frontend/src/app/features/projects/services/projects-facade.service.ts
- [ ] T090 [US2] Интегрировать WebSocket подписку на /topic/project.{projectId} в ProjectsFacade

### Frontend: Components (Presentational)

- [ ] T091 [P] [US2] Создать ProjectCardComponent (Dumb) с @Input project в frontend/src/app/features/projects/components/project-card.component.ts
- [ ] T092 [P] [US2] Создать ProjectFormComponent (Dumb) с Reactive Form (название, описание, color picker, dates) в frontend/src/app/features/projects/components/project-form.component.ts
- [ ] T093 [P] [US2] Создать ProjectMembersListComponent (Dumb) с drag & drop для порядка в frontend/src/app/features/projects/components/project-members-list.component.ts
- [ ] T094 [P] [US2] Создать AddMemberDialogComponent (Dumb) с email autocomplete в frontend/src/app/features/projects/components/add-member-dialog.component.ts

### Frontend: Components (Smart/Container)

- [ ] T095 [US2] Создать ProjectListContainerComponent (Smart) в frontend/src/app/features/projects/containers/project-list-container.component.ts
- [ ] T096 [US2] Создать ProjectDetailsContainerComponent (Smart) с управлением участниками в frontend/src/app/features/projects/containers/project-details-container.component.ts
- [ ] T097 [US2] Создать ProjectSettingsContainerComponent (Smart) в frontend/src/app/features/projects/containers/project-settings-container.component.ts

### Frontend: Routing & Validation

- [ ] T098 [US2] Настроить projects routing module в frontend/src/app/features/projects/projects-routing.module.ts
- [ ] T099 [P] [US2] Создать ProjectsModule в frontend/src/app/features/projects/projects.module.ts
- [ ] T100 [US2] Добавить custom validators для ProjectFormComponent (dates validation, color format) в frontend/src/app/features/projects/validators/

### Integration & Testing

- [ ] T101 [US2] Написать integration тест для ProjectController в backend/src/test/java/com/sprinto/tms/integration/ProjectControllerTest.java
- [ ] T102 [US2] Написать integration тест для ProjectMembersController в backend/src/test/java/com/sprinto/tms/integration/ProjectMembersControllerTest.java
- [ ] T103 [P] [US2] Написать unit тест для ProjectService в backend/src/test/java/com/sprinto/tms/unit/ProjectServiceTest.java

---

## Phase 5: User Story 3 - Личный дашборд и планирование (P2)

**Цель**: Реализовать личный дашборд с виджетами, календарь, планирование ресурсов.

**Критерии завершения**:
- ✅ Пользователь видит дашборд с активными задачами, статистикой часов, дедлайнами
- ✅ Виджеты можно переставлять через drag & drop, компоновка сохраняется
- ✅ Календарь отображает задачи с color coding по проектам
- ✅ График загруженности показывает предупреждение о перегрузке
- ✅ Countdown таймер для задач с дедлайном <24 часов

### Backend: Services & Endpoints

- [ ] T104 [P] [US3] Создать DashboardService для агрегации данных дашборда в backend/src/main/java/com/sprinto/tms/service/DashboardService.java
- [ ] T105 [P] [US3] Создать DashboardDTO с виджетами данными в backend/src/main/java/com/sprinto/tms/dto/dashboard/DashboardDTO.java
- [ ] T106 [US3] Реализовать DashboardController (GET /api/dashboard/personal) в backend/src/main/java/com/sprinto/tms/api/rest/DashboardController.java
- [ ] T107 [US3] Создать UserSettingsService для сохранения компоновки виджетов в backend/src/main/java/com/sprinto/tms/service/UserSettingsService.java
- [ ] T108 [US3] Реализовать UserSettingsController (PUT /api/users/me/settings) в backend/src/main/java/com/sprinto/tms/api/rest/UserSettingsController.java

### Frontend: Dashboard Store & Services

- [ ] T109 [US3] Создать dashboard feature store в frontend/src/app/features/dashboard/store/
- [ ] T110 [P] [US3] Создать DashboardApiService в frontend/src/app/features/dashboard/services/dashboard-api.service.ts
- [ ] T111 [US3] Создать DashboardFacade в frontend/src/app/features/dashboard/services/dashboard-facade.service.ts

### Frontend: Calendar Integration

- [ ] T112 [P] [US3] Установить и настроить FullCalendar Angular в frontend/package.json
- [ ] T113 [P] [US3] Создать CalendarComponent с интеграцией FullCalendar в frontend/src/app/features/dashboard/components/calendar.component.ts
- [ ] T114 [US3] Добавить drag & drop задач в CalendarComponent

### Frontend: Dashboard Widgets (Presentational)

- [ ] T115 [P] [US3] Создать ActiveTasksWidgetComponent (Dumb) в frontend/src/app/features/dashboard/components/widgets/active-tasks-widget.component.ts
- [ ] T116 [P] [US3] Создать HoursWorkedChartWidgetComponent (Dumb) с Chart.js интеграцией в frontend/src/app/features/dashboard/components/widgets/hours-worked-chart-widget.component.ts
- [ ] T117 [P] [US3] Создать DeadlinesWidgetComponent (Dumb) с countdown таймерами в frontend/src/app/features/dashboard/components/widgets/deadlines-widget.component.ts
- [ ] T118 [P] [US3] Создать ProjectsOverviewWidgetComponent (Dumb) в frontend/src/app/features/dashboard/components/widgets/projects-overview-widget.component.ts
- [ ] T119 [P] [US3] Создать WorkloadGraphComponent (Dumb) с bar chart для загруженности в frontend/src/app/features/dashboard/components/workload-graph.component.ts

### Frontend: Dashboard Container & Layout

- [ ] T120 [US3] Создать DashboardContainerComponent (Smart) с подпиской на store в frontend/src/app/features/dashboard/containers/dashboard-container.component.ts
- [ ] T121 [US3] Реализовать drag & drop grid layout для виджетов в DashboardContainerComponent (Angular CDK Drag & Drop)
- [ ] T122 [US3] Добавить сохранение компоновки виджетов через UserSettingsService

### Frontend: Module & Routing

- [ ] T123 [US3] Создать DashboardModule в frontend/src/app/features/dashboard/dashboard.module.ts
- [ ] T124 [US3] Настроить dashboard routing в frontend/src/app/features/dashboard/dashboard-routing.module.ts

### Testing

- [ ] T125 [US3] Написать integration тест для DashboardController в backend/src/test/java/com/sprinto/tms/integration/DashboardControllerTest.java
- [ ] T126 [P] [US3] Написать unit тест для DashboardService в backend/src/test/java/com/sprinto/tms/unit/DashboardServiceTest.java

---

## Phase 6: User Story 4 - Отчеты и аналитика (P2)

**Цель**: Реализовать генерацию отчетов, интерактивную аналитику, экспорт в Excel/CSV.

**Критерии завершения**:
- ✅ Владелец проекта может сгенерировать отчет по проекту за период
- ✅ Отчет содержит интерактивные графики (pie, bar, line charts)
- ✅ Drill-down: клик на график показывает детали
- ✅ Система рассчитывает стоимость работ по приоритету ставок
- ✅ Экспорт отчета в Excel работает и скачивает файл
- ✅ Отклонение от оценки отображается в процентах

### Backend: Services & DTOs

- [ ] T127 [P] [US4] Создать ReportService для агрегации данных в backend/src/main/java/com/sprinto/tms/service/ReportService.java
- [ ] T128 [P] [US4] Создать ProjectReportDTO, TaskReportDTO, SummaryReportDTO, PersonalReportDTO в backend/src/main/java/com/sprinto/tms/dto/report/
- [ ] T129 [US4] Реализовать методы генерации отчетов в ReportService (project, task, summary, personal reports)
- [ ] T130 [US4] Создать ExcelExportService с Apache POI для генерации .xlsx в backend/src/main/java/com/sprinto/tms/service/ExcelExportService.java
- [ ] T131 [US4] Создать CsvExportService для экспорта в CSV в backend/src/main/java/com/sprinto/tms/service/CsvExportService.java

### Backend: Controllers

- [ ] T132 [US4] Реализовать ReportsController (GET /api/reports/project/{id}, /api/reports/task, /api/reports/summary) в backend/src/main/java/com/sprinto/tms/api/rest/ReportsController.java
- [ ] T133 [US4] Реализовать ExportController (GET /api/reports/export/excel, /api/reports/export/csv) с streaming download в backend/src/main/java/com/sprinto/tms/api/rest/ExportController.java

### Frontend: Reports Store & Services

- [ ] T134 [US4] Создать reports feature store в frontend/src/app/features/reports/store/
- [ ] T135 [P] [US4] Создать ReportsApiService в frontend/src/app/features/reports/services/reports-api.service.ts
- [ ] T136 [US4] Создать ReportsFacade в frontend/src/app/features/reports/services/reports-facade.service.ts

### Frontend: Charts & Visualization

- [ ] T137 [P] [US4] Установить Chart.js и ng2-charts в frontend/package.json
- [ ] T138 [P] [US4] Создать ProjectReportChartComponent (Dumb) с pie/bar charts в frontend/src/app/features/reports/components/project-report-chart.component.ts
- [ ] T139 [P] [US4] Создать TaskReportTimelineComponent (Dumb) в frontend/src/app/features/reports/components/task-report-timeline.component.ts
- [ ] T140 [P] [US4] Создать CostBreakdownChartComponent (Dumb) для биллинга в frontend/src/app/features/reports/components/cost-breakdown-chart.component.ts
- [ ] T141 [US4] Реализовать drill-down логику в charts (клик → детализация)

### Frontend: Report Components

- [ ] T142 [P] [US4] Создать ReportFiltersComponent (Dumb) с date range picker, project selector в frontend/src/app/features/reports/components/report-filters.component.ts
- [ ] T143 [P] [US4] Создать ExportButtonsComponent (Dumb) в frontend/src/app/features/reports/components/export-buttons.component.ts

### Frontend: Containers

- [ ] T144 [US4] Создать ProjectReportContainerComponent (Smart) в frontend/src/app/features/reports/containers/project-report-container.component.ts
- [ ] T145 [US4] Создать TaskReportContainerComponent (Smart) в frontend/src/app/features/reports/containers/task-report-container.component.ts
- [ ] T146 [US4] Создать SummaryReportContainerComponent (Smart) в frontend/src/app/features/reports/containers/summary-report-container.component.ts
- [ ] T147 [US4] Создать PersonalReportContainerComponent (Smart) в frontend/src/app/features/reports/containers/personal-report-container.component.ts

### Frontend: Module & Routing

- [ ] T148 [US4] Создать ReportsModule в frontend/src/app/features/reports/reports.module.ts
- [ ] T149 [US4] Настроить reports routing в frontend/src/app/features/reports/reports-routing.module.ts

### Testing

- [ ] T150 [US4] Написать integration тест для ReportsController в backend/src/test/java/com/sprinto/tms/integration/ReportsControllerTest.java
- [ ] T151 [P] [US4] Написать unit тест для ExcelExportService в backend/src/test/java/com/sprinto/tms/unit/ExcelExportServiceTest.java

---

## Phase 7: User Story 5 - Система уведомлений (P2)

**Цель**: Реализовать multi-channel уведомления (in-app, email, push, WebSocket).

**Критерии завершения**:
- ✅ При назначении задачи участник получает уведомления через выбранные каналы
- ✅ In-app уведомления отображаются с badge количества
- ✅ Email уведомления отправляются с настраиваемыми шаблонами
- ✅ Push notifications работают через Browser Push API
- ✅ WebSocket доставляет уведомления в реальном времени
- ✅ Режим "Не беспокоить" блокирует email/push, но сохраняет in-app
- ✅ Настройки уведомлений сохраняются для каждого типа и канала

### Backend: Domain & Repository

- [ ] T152 [P] [US5] Создать domain entity Notification в backend/src/main/java/com/sprinto/tms/domain/notification/Notification.java
- [ ] T153 [P] [US5] Создать NotificationRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/NotificationRepository.java

### Backend: Services

- [ ] T154 [US5] Создать NotificationService с методами create, send, markAsRead в backend/src/main/java/com/sprinto/tms/service/NotificationService.java
- [ ] T155 [US5] Создать EmailNotificationService с Spring Mail и Thymeleaf templates в backend/src/main/java/com/sprinto/tms/service/EmailNotificationService.java
- [ ] T156 [US5] Создать PushNotificationService с Web Push library в backend/src/main/java/com/sprinto/tms/service/PushNotificationService.java
- [ ] T157 [US5] Создать NotificationRouter для routing по каналам на основе user preferences в backend/src/main/java/com/sprinto/tms/service/NotificationRouter.java

### Backend: Resilience & Graceful Degradation (Notifications)

- [ ] T633 [US5] Добавить обработку ошибок и таймаутов внешнего почтового сервиса в EmailNotificationService с логированием и безопасным отказом, не блокирующим бизнес‑операции
- [ ] T634 [P] [US5] Написать integration тест для сценариев деградации email‑уведомлений в backend/src/test/java/com/sprinto/tms/integration/EmailNotificationResilienceTest.java

### Backend: WebSocket Handler

- [ ] T158 [US5] Создать NotificationWebSocketHandler для отправки на /user/queue/notifications в backend/src/main/java/com/sprinto/tms/api/websocket/NotificationWebSocketHandler.java
- [ ] T159 [US5] Интегрировать NotificationWebSocketHandler в NotificationService

### Backend: Email Templates

- [ ] T160 [P] [US5] Создать Thymeleaf email template для TASK_ASSIGNED в backend/src/main/resources/templates/email/task-assigned.html
- [ ] T161 [P] [US5] Создать Thymeleaf email template для DEADLINE_APPROACHING в backend/src/main/resources/templates/email/deadline-approaching.html
- [ ] T162 [P] [US5] Создать Thymeleaf email template для MENTIONED в backend/src/main/resources/templates/email/mentioned.html

### Backend: Controllers

- [ ] T163 [US5] Реализовать NotificationsController (GET /api/notifications, PUT /api/notifications/{id}/read, PUT /api/notifications/read-all) в backend/src/main/java/com/sprinto/tms/api/rest/NotificationsController.java
- [ ] T164 [US5] Реализовать NotificationSettingsController (GET /api/users/me/notification-settings, PUT /api/users/me/notification-settings) в backend/src/main/java/com/sprinto/tms/api/rest/NotificationSettingsController.java

### Frontend: Notifications Store & Services

- [ ] T165 [US5] Создать notifications feature store в frontend/src/app/features/notifications/store/
- [ ] T166 [P] [US5] Создать NotificationsApiService в frontend/src/app/features/notifications/services/notifications-api.service.ts
- [ ] T167 [US5] Создать NotificationsFacade в frontend/src/app/features/notifications/services/notifications-facade.service.ts
- [ ] T168 [US5] Интегрировать WebSocket подписку на /user/queue/notifications в NotificationsFacade

### Frontend: Push Notifications

- [ ] T169 [US5] Создать PushNotificationService для регистрации service worker в frontend/src/app/core/services/push-notification.service.ts
- [ ] T170 [US5] Создать service worker файл для browser push в frontend/src/firebase-messaging-sw.js (или custom SW)

### Frontend: Components (Presentational)

- [ ] T171 [P] [US5] Создать NotificationBadgeComponent (Dumb) для отображения количества в frontend/src/app/shared/components/notification-badge.component.ts
- [ ] T172 [P] [US5] Создать NotificationDropdownComponent (Dumb) с списком уведомлений в frontend/src/app/features/notifications/components/notification-dropdown.component.ts
- [ ] T173 [P] [US5] Создать NotificationItemComponent (Dumb) в frontend/src/app/features/notifications/components/notification-item.component.ts
- [ ] T174 [P] [US5] Создать NotificationSettingsComponent (Dumb) с toggle switches в frontend/src/app/features/notifications/components/notification-settings.component.ts
- [ ] T175 [P] [US5] Создать DoNotDisturbToggleComponent (Dumb) в frontend/src/app/features/notifications/components/do-not-disturb-toggle.component.ts

### Frontend: Containers

- [ ] T176 [US5] Создать NotificationsContainerComponent (Smart) в frontend/src/app/features/notifications/containers/notifications-container.component.ts
- [ ] T177 [US5] Создать NotificationSettingsContainerComponent (Smart) в frontend/src/app/features/notifications/containers/notification-settings-container.component.ts

### Frontend: Module

- [ ] T178 [US5] Создать NotificationsModule в frontend/src/app/features/notifications/notifications.module.ts

### Testing

- [ ] T179 [US5] Написать integration тест для NotificationsController в backend/src/test/java/com/sprinto/tms/integration/NotificationsControllerTest.java
- [ ] T180 [P] [US5] Написать unit тест для NotificationService в backend/src/test/java/com/sprinto/tms/unit/NotificationServiceTest.java
- [ ] T181 [P] [US5] Написать unit тест для EmailNotificationService в backend/src/test/java/com/sprinto/tms/unit/EmailNotificationServiceTest.java

---

## Phase 8: User Story 6 - Интеграция с Google Calendar (P3)

**Цель**: Реализовать OAuth 2.0 авторизацию и синхронизацию задач с Google Calendar.

**Критерии завершения**:
- ✅ Пользователь может подключить Google Calendar через OAuth 2.0 popup
- ✅ После авторизации доступен список календарей для выбора
- ✅ Задачи с дедлайнами экспортируются в выбранный календарь с префиксом [TMS]
- ✅ Цвет проекта применяется к событиям в календаре
- ✅ Фильтры синхронизации (по приоритетам, проектам) работают
- ✅ Ручная синхронизация выполняется по кнопке

### Backend: Google Calendar Integration

- [ ] T182 [P] [US6] Добавить Google Calendar API client library в backend/build.gradle.kts
- [ ] T183 [P] [US6] Создать domain entity CalendarSettings в backend/src/main/java/com/sprinto/tms/domain/calendar/CalendarSettings.java
- [ ] T184 [P] [US6] Создать CalendarSettingsRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/CalendarSettingsRepository.java
- [ ] T185 [US6] Создать GoogleCalendarService с методами authenticate, listCalendars, syncTask в backend/src/main/java/com/sprinto/tms/service/GoogleCalendarService.java
- [ ] T186 [US6] Создать CalendarSyncScheduler для периодической синхронизации (@Scheduled) в backend/src/main/java/com/sprinto/tms/service/CalendarSyncScheduler.java

### Backend: OAuth 2.0 Flow

- [ ] T187 [US6] Реализовать GoogleOAuthController (GET /api/calendar/oauth/authorize, GET /api/calendar/oauth/callback) в backend/src/main/java/com/sprinto/tms/api/rest/GoogleOAuthController.java
- [ ] T188 [US6] Создать метод обмена authorization code на tokens в GoogleCalendarService
- [ ] T189 [US6] Реализовать шифрование access_token и refresh_token (AES-256) в GoogleCalendarService
- [ ] T190 [US6] Реализовать refresh token rotation в GoogleCalendarService

### Backend: Calendar Sync

- [ ] T191 [US6] Реализовать CalendarController (GET /api/calendar/settings, PUT /api/calendar/settings, POST /api/calendar/sync) в backend/src/main/java/com/sprinto/tms/api/rest/CalendarController.java
- [ ] T192 [US6] Создать CalendarEventMapper для Task → Google Calendar Event в backend/src/main/java/com/sprinto/tms/mapper/CalendarEventMapper.java
- [ ] T193 [US6] Реализовать фильтрацию задач на основе sync_filters в GoogleCalendarService

### Backend: Resilience & Graceful Degradation (Google Calendar)

- [ ] T631 [US6] Реализовать обработку недоступности Google Calendar API (таймауты, коды ошибок) с логированием, безопасным отключением синхронизации и уведомлением пользователя без блокировки основной работы с задачами
- [ ] T632 [P] [US6] Написать integration тесты для CalendarController/GoogleCalendarService с эмуляцией недоступности внешнего сервиса (5xx/timeout) в backend/src/test/java/com/sprinto/tms/integration/GoogleCalendarResilienceTest.java

### Frontend: Calendar Store & Services

- [ ] T194 [US6] Создать calendar feature store в frontend/src/app/features/calendar/store/
- [ ] T195 [P] [US6] Создать CalendarApiService в frontend/src/app/features/calendar/services/calendar-api.service.ts
- [ ] T196 [US6] Создать CalendarFacade в frontend/src/app/features/calendar/services/calendar-facade.service.ts

### Frontend: OAuth Flow

- [ ] T197 [US6] Создать GoogleAuthService для OAuth popup window в frontend/src/app/features/calendar/services/google-auth.service.ts
- [ ] T198 [US6] Реализовать OAuth callback handler в frontend

### Frontend: Components

- [ ] T199 [P] [US6] Создать ConnectCalendarButtonComponent (Dumb) в frontend/src/app/features/calendar/components/connect-calendar-button.component.ts
- [ ] T200 [P] [US6] Создать CalendarSelectorComponent (Dumb) с dropdown списком календарей в frontend/src/app/features/calendar/components/calendar-selector.component.ts
- [ ] T201 [P] [US6] Создать SyncFiltersComponent (Dumb) с checkboxes для приоритетов и проектов в frontend/src/app/features/calendar/components/sync-filters.component.ts
- [ ] T202 [P] [US6] Создать ManualSyncButtonComponent (Dumb) в frontend/src/app/features/calendar/components/manual-sync-button.component.ts

### Frontend: Containers

- [ ] T203 [US6] Создать CalendarIntegrationContainerComponent (Smart) в frontend/src/app/features/calendar/containers/calendar-integration-container.component.ts

### Frontend: Module

- [ ] T204 [US6] Создать CalendarModule в frontend/src/app/features/calendar/calendar.module.ts

### Testing

- [ ] T205 [US6] Написать integration тест для GoogleOAuthController в backend/src/test/java/com/sprinto/tms/integration/GoogleOAuthControllerTest.java
- [ ] T206 [P] [US6] Написать unit тест для GoogleCalendarService в backend/src/test/java/com/sprinto/tms/unit/GoogleCalendarServiceTest.java

---

## Phase 9: User Story 7 - BI Dashboard для анализа эффективности (P3)

**Цель**: Реализовать административный дашборд и BI аналитику с метриками эффективности.

**Критерии завершения**:
- ✅ SYSTEM_ADMIN видит административный дашборд с системной статистикой
- ✅ Leaderboard показывает топ-10 пользователей по часам/эффективности
- ✅ Area chart отображает загруженность по проектам и времени
- ✅ Burndown chart показывает прогресс выполнения проекта
- ✅ Gauges отображают метрики эффективности (% завершенных в срок)
- ✅ Alerts показывают проблемные зоны

### Backend: Analytics Services

- [ ] T207 [US7] Создать AnalyticsService для расчета метрик эффективности в backend/src/main/java/com/sprinto/tms/service/AnalyticsService.java
- [ ] T208 [US7] Создать LeaderboardService для топ пользователей в backend/src/main/java/com/sprinto/tms/service/LeaderboardService.java
- [ ] T209 [US7] Создать BurndownChartService для расчета burndown данных в backend/src/main/java/com/sprinto/tms/service/BurndownChartService.java

### Backend: DTOs & Controllers

- [ ] T210 [P] [US7] Создать AdminDashboardDTO, LeaderboardEntryDTO, MetricDTO в backend/src/main/java/com/sprinto/tms/dto/analytics/
- [ ] T211 [US7] Реализовать AnalyticsController (GET /api/analytics/admin-dashboard, GET /api/analytics/leaderboard) в backend/src/main/java/com/sprinto/tms/api/rest/AnalyticsController.java
- [ ] T212 [US7] Реализовать ProjectAnalyticsController (GET /api/projects/{id}/burndown, GET /api/projects/{id}/metrics) в backend/src/main/java/com/sprinto/tms/api/rest/ProjectAnalyticsController.java

### Frontend: Analytics Store & Services

- [ ] T213 [US7] Создать analytics feature store в frontend/src/app/features/analytics/store/
- [ ] T214 [P] [US7] Создать AnalyticsApiService в frontend/src/app/features/analytics/services/analytics-api.service.ts
- [ ] T215 [US7] Создать AnalyticsFacade в frontend/src/app/features/analytics/services/analytics-facade.service.ts

### Frontend: BI Widgets & Charts

- [ ] T216 [P] [US7] Создать SystemStatsCardsComponent (Dumb) в frontend/src/app/features/analytics/components/system-stats-cards.component.ts
- [ ] T217 [P] [US7] Создать LeaderboardComponent (Dumb) с таблицей топ-10 в frontend/src/app/features/analytics/components/leaderboard.component.ts
- [ ] T218 [P] [US7] Создать WorkloadAreaChartComponent (Dumb) в frontend/src/app/features/analytics/components/workload-area-chart.component.ts
- [ ] T219 [P] [US7] Создать BurndownChartComponent (Dumb) в frontend/src/app/features/analytics/components/burndown-chart.component.ts
- [ ] T220 [P] [US7] Создать EfficiencyGaugesComponent (Dumb) в frontend/src/app/features/analytics/components/efficiency-gauges.component.ts
- [ ] T221 [P] [US7] Создать ProblemsAlertsComponent (Dumb) в frontend/src/app/features/analytics/components/problems-alerts.component.ts

### Frontend: Containers

- [ ] T222 [US7] Создать AdminDashboardContainerComponent (Smart) с ролью SYSTEM_ADMIN в frontend/src/app/features/analytics/containers/admin-dashboard-container.component.ts
- [ ] T223 [US7] Создать ProjectDashboardContainerComponent (Smart) в frontend/src/app/features/analytics/containers/project-dashboard-container.component.ts

### Frontend: Module & Guards

- [ ] T224 [US7] Создать AnalyticsModule в frontend/src/app/features/analytics/analytics.module.ts
- [ ] T225 [US7] Создать AdminGuard для защиты admin routes в frontend/src/app/core/guards/admin.guard.ts

### Testing

- [ ] T226 [US7] Написать integration тест для AnalyticsController в backend/src/test/java/com/sprinto/tms/integration/AnalyticsControllerTest.java

---

## Phase 10: User Story 8 - Система рейтинга команды (P3)

**Цель**: Реализовать расчет personal productivity score и рейтинг команды.

**Критерии завершения**:
- ✅ Пользователь видит свой productivity score на основе метрик
- ✅ Сравнение с командой отображается без раскрытия личных данных
- ✅ При завершении задачи в срок начисляются баллы
- ✅ Leaderboard команды отображается с учетом настроек приватности

### Backend: Rating Services

- [ ] T227 [US8] Создать ProductivityScoreService для расчета score в backend/src/main/java/com/sprinto/tms/service/ProductivityScoreService.java
- [ ] T228 [US8] Создать RatingService для управления рейтингом в backend/src/main/java/com/sprinto/tms/service/RatingService.java
- [ ] T229 [US8] Реализовать @Scheduled job для ежедневного пересчета productivity score в RatingService

### Backend: DTOs & Controllers

- [ ] T230 [P] [US8] Создать ProductivityScoreDTO, TeamRatingDTO в backend/src/main/java/com/sprinto/tms/dto/rating/
- [ ] T231 [US8] Реализовать RatingController (GET /api/rating/me, GET /api/rating/team) в backend/src/main/java/com/sprinto/tms/api/rest/RatingController.java

### Backend: Privacy & Leaderboard Anonymization

- [ ] T644 [US8] Расширить UserSettings entity и схему хранения (NFR-050, FR-114.1) полями настроек приватности рейтинга (видимость в leaderboard, режим анонимизации, отображаемое имя) в backend/src/main/java/com/sprinto/tms/domain/user/UserSettings.java
- [ ] T645 [US8] Обновить RatingService/AnalyticsService для применения настроек приватности при формировании командного рейтинга и метрик (скрытие пользователей, анонимизация данных) в backend/src/main/java/com/sprinto/tms/service/RatingService.java
- [ ] T646 [P] [US8] Написать integration тест, проверяющий соблюдение приватности и анонимизации в RatingController/AnalyticsController (скрытие/обезличивание записей) в backend/src/test/java/com/sprinto/tms/integration/RatingPrivacyTest.java

### Frontend: Rating Store & Services

- [ ] T232 [US8] Создать rating feature store в frontend/src/app/features/rating/store/
- [ ] T233 [P] [US8] Создать RatingApiService в frontend/src/app/features/rating/services/rating-api.service.ts
- [ ] T234 [US8] Создать RatingFacade в frontend/src/app/features/rating/services/rating-facade.service.ts

### Frontend: Components

- [ ] T235 [P] [US8] Создать ProductivityScoreDisplayComponent (Dumb) в frontend/src/app/features/rating/components/productivity-score-display.component.ts
- [ ] T236 [P] [US8] Создать TeamComparisonChartComponent (Dumb) в frontend/src/app/features/rating/components/team-comparison-chart.component.ts
- [ ] T237 [P] [US8] Создать TeamLeaderboardComponent (Dumb) в frontend/src/app/features/rating/components/team-leaderboard.component.ts

### Frontend: Rating Privacy Settings

- [ ] T647 [US8] Создать RatingPrivacySettingsComponent (Dumb) для управления настройками приватности рейтинга (видимость, режим анонимизации, отображаемое имя) в frontend/src/app/features/rating/components/rating-privacy-settings.component.ts
- [ ] T648 [P] [US8] Обновить RatingContainerComponent и связанные фасады для загрузки/сохранения настроек приватности и передачи их в компоненты рейтинга в frontend/src/app/features/rating/containers/rating-container.component.ts
- [ ] T649 [P] [US8] Обновить TeamLeaderboardComponent для отображения анонимизированных данных и скрытия пользователей согласно их настройкам приватности; написать unit тесты для проверки этих сценариев в frontend/src/app/features/rating/components/team-leaderboard.component.spec.ts

### Frontend: Containers

- [ ] T238 [US8] Создать RatingContainerComponent (Smart) в frontend/src/app/features/rating/containers/rating-container.component.ts

### Frontend: Module

- [ ] T239 [US8] Создать RatingModule в frontend/src/app/features/rating/rating.module.ts

---

## Phase 11: User Story 9 - Иерархия задач и зависимости (P3)

**Цель**: Реализовать подзадачи до 5 уровней и зависимости между задачами.

**Критерии завершения**:
- ✅ Пользователь может создать подзадачу, указывая родителя
- ✅ Tree view отображает иерархию с отступами
- ✅ Индикатор прогресса родительской задачи обновляется при завершении подзадач
- ✅ Drag & drop изменяет иерархию с соблюдением limit 5 уровней
- ✅ Зависимости между задачами блокируют начало работы
- ✅ При попытке завершить задачу с незавершенными подзадачами показывается предупреждение

### Backend: Task Hierarchy

- [ ] T240 [US9] Добавить методы работы с иерархией в TaskService (createSubtask, moveTask, getSubtasks) в backend/src/main/java/com/sprinto/tms/service/TaskService.java
- [ ] T241 [US9] Реализовать валидацию max hierarchy level (5 уровней) в TaskService
- [ ] T242 [US9] Создать метод calculateProgress для родительских задач в TaskService

### Backend: Task Dependencies

- [ ] T243 [P] [US9] Создать domain entity TaskDependency в backend/src/main/java/com/sprinto/tms/domain/task/TaskDependency.java
- [ ] T244 [P] [US9] Создать TaskDependencyRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/TaskDependencyRepository.java
- [ ] T245 [US9] Создать TaskDependencyService с методами addDependency, removeDependency, checkCircular в backend/src/main/java/com/sprinto/tms/service/TaskDependencyService.java
- [ ] T246 [US9] Реализовать алгоритм проверки циклических зависимостей в TaskDependencyService

### Backend: Controllers

- [ ] T247 [US9] Добавить endpoints для иерархии в TaskController (POST /api/tasks/{id}/subtasks, PUT /api/tasks/{id}/move) в backend/src/main/java/com/sprinto/tms/api/rest/TaskController.java
- [ ] T248 [US9] Реализовать TaskDependenciesController (POST /api/tasks/{id}/dependencies, DELETE /api/tasks/{id}/dependencies/{blockingTaskId}) в backend/src/main/java/com/sprinto/tms/api/rest/TaskDependenciesController.java

### Frontend: Task Hierarchy Components

- [ ] T249 [P] [US9] Создать TaskTreeViewComponent (Dumb) с tree rendering в frontend/src/app/features/tasks/components/task-tree-view.component.ts
- [ ] T250 [US9] Добавить drag & drop для изменения иерархии в TaskTreeViewComponent (Angular CDK Tree)
- [ ] T251 [P] [US9] Создать TaskProgressIndicatorComponent (Dumb) для родительских задач в frontend/src/app/shared/components/task-progress-indicator.component.ts
- [ ] T252 [P] [US9] Создать SubtaskFormComponent (Dumb) в frontend/src/app/features/tasks/components/subtask-form.component.ts

### Frontend: Task Dependencies Components

- [ ] T253 [P] [US9] Создать TaskDependenciesListComponent (Dumb) в frontend/src/app/features/tasks/components/task-dependencies-list.component.ts
- [ ] T254 [P] [US9] Создать AddDependencyDialogComponent (Dumb) в frontend/src/app/features/tasks/components/add-dependency-dialog.component.ts
- [ ] T255 [P] [US9] Создать BlockedTaskIndicatorComponent (Dumb) в frontend/src/app/features/tasks/components/blocked-task-indicator.component.ts

### Frontend: Services & Store Updates

- [ ] T256 [US9] Добавить методы для иерархии в TasksApiService (createSubtask, moveTask) в frontend/src/app/features/tasks/services/tasks-api.service.ts
- [ ] T257 [US9] Добавить методы для зависимостей в TasksApiService (addDependency, removeDependency) в frontend/src/app/features/tasks/services/tasks-api.service.ts
- [ ] T258 [US9] Обновить tasks store для поддержки иерархии (tree structure в state) в frontend/src/app/features/tasks/store/

### Testing

- [ ] T259 [US9] Написать integration тест для иерархии задач в backend/src/test/java/com/sprinto/tms/integration/TaskHierarchyTest.java
- [ ] T260 [US9] Написать integration тест для зависимостей задач в backend/src/test/java/com/sprinto/tms/integration/TaskDependenciesTest.java

---

## Phase 4.5: Extended Task Features - Комментарии, Метки и Файлы (P1)

**Цель**: Реализовать расширенные возможности для задач - комментарии, метки/теги и файловые вложения.

**Обоснование приоритета**: Эти функции критичны для полноценной работы с задачами и должны быть реализованы сразу после базовых возможностей (US1, US2).

**Критерии завершения**:
- ✅ Пользователи могут оставлять комментарии к задачам
- ✅ Поддержка упоминаний через @username в комментариях
- ✅ Пользователи могут создавать и присваивать метки задачам
- ✅ Метки доступны глобально и на уровне проекта
- ✅ Файлы можно прикреплять к задачам через drag & drop
- ✅ Preview для изображений работает
- ✅ Контроль размера файлов (max 10 МБ)

### Backend: Comments Domain & Repository

- [ ] T298 [P] [EXT] Создать domain entity Comment в backend/src/main/java/com/sprinto/tms/domain/comment/Comment.java
- [ ] T299 [P] [EXT] Создать CommentRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/CommentRepository.java

### Backend: Comments Services & DTOs

- [ ] T300 [P] [EXT] Создать CommentDTO, CommentCreateDTO, CommentResponseDTO в backend/src/main/java/com/sprinto/tms/dto/comment/
- [ ] T301 [P] [EXT] Создать CommentMapper (MapStruct) в backend/src/main/java/com/sprinto/tms/mapper/CommentMapper.java
- [ ] T302 [EXT] Реализовать CommentService с методами create, findByTaskId, delete, parseMentions в backend/src/main/java/com/sprinto/tms/service/CommentService.java
- [ ] T303 [EXT] Реализовать MentionService для обработки @username упоминаний в backend/src/main/java/com/sprinto/tms/service/MentionService.java

### Backend: Comments Controllers

- [ ] T304 [EXT] Реализовать CommentsController (POST /api/tasks/{taskId}/comments, GET /api/tasks/{taskId}/comments, DELETE /api/comments/{id}) в backend/src/main/java/com/sprinto/tms/api/rest/CommentsController.java
- [ ] T305 [EXT] Интегрировать отправку уведомлений при упоминании пользователя в CommentService

### Backend: Tags/Labels Domain & Repository

- [ ] T306 [P] [EXT] Создать domain entity Tag в backend/src/main/java/com/sprinto/tms/domain/tag/Tag.java
- [ ] T307 [P] [EXT] Создать domain entity TaskTag (связь many-to-many) в backend/src/main/java/com/sprinto/tms/domain/task/TaskTag.java
- [ ] T308 [P] [EXT] Создать TagRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/TagRepository.java
- [ ] T309 [P] [EXT] Создать TaskTagRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/TaskTagRepository.java

### Backend: Tags Services & DTOs

- [ ] T310 [P] [EXT] Создать TagDTO, TagCreateDTO в backend/src/main/java/com/sprinto/tms/dto/tag/
- [ ] T311 [P] [EXT] Создать TagMapper (MapStruct) в backend/src/main/java/com/sprinto/tms/mapper/TagMapper.java
- [ ] T312 [EXT] Реализовать TagService с методами create, findAll, findByProject, assignToTask, removeFromTask в backend/src/main/java/com/sprinto/tms/service/TagService.java

### Backend: Tags Controllers

- [ ] T313 [EXT] Реализовать TagsController (POST /api/tags, GET /api/tags, GET /api/projects/{id}/tags) в backend/src/main/java/com/sprinto/tms/api/rest/TagsController.java
- [ ] T314 [EXT] Реализовать TaskTagsController (POST /api/tasks/{taskId}/tags, DELETE /api/tasks/{taskId}/tags/{tagId}) в backend/src/main/java/com/sprinto/tms/api/rest/TaskTagsController.java

### Backend: Files Domain & Repository

- [ ] T315 [P] [EXT] Создать domain entity File в backend/src/main/java/com/sprinto/tms/domain/file/File.java
- [ ] T316 [P] [EXT] Создать FileRepository (R2DBC) в backend/src/main/java/com/sprinto/tms/repository/FileRepository.java

### Backend: Files Services & Storage

- [ ] T317 [P] [EXT] Создать FileDTO, FileUploadResponse в backend/src/main/java/com/sprinto/tms/dto/file/
- [ ] T318 [EXT] Реализовать FileStorageService для сохранения файлов (локальное хранилище или S3) в backend/src/main/java/com/sprinto/tms/service/FileStorageService.java
- [ ] T319 [EXT] Реализовать FileService с методами upload, download, delete, validateSize, validateType в backend/src/main/java/com/sprinto/tms/service/FileService.java
- [ ] T320 [EXT] Реализовать FileQuotaService для проверки лимитов в backend/src/main/java/com/sprinto/tms/service/FileQuotaService.java

### Backend: Files Controllers

- [ ] T321 [EXT] Реализовать FilesController (POST /api/tasks/{taskId}/files, GET /api/files/{id}, DELETE /api/files/{id}) с multipart upload в backend/src/main/java/com/sprinto/tms/api/rest/FilesController.java
- [ ] T322 [EXT] Реализовать FileDownloadController (GET /api/files/{id}/download) с streaming в backend/src/main/java/com/sprinto/tms/api/rest/FileDownloadController.java

### Frontend: Comments Models & Store

- [ ] T323 [P] [EXT] Создать Comment model interface в frontend/src/app/shared/models/comment.model.ts
- [ ] T324 [EXT] Создать comments feature store (actions, reducer, effects, selectors) в frontend/src/app/features/comments/store/
- [ ] T325 [P] [EXT] Создать CommentsApiService в frontend/src/app/features/comments/services/comments-api.service.ts
- [ ] T326 [EXT] Создать CommentsFacade в frontend/src/app/features/comments/services/comments-facade.service.ts

### Frontend: Comments Components

- [ ] T327 [P] [EXT] Создать CommentListComponent (Dumb) с хронологическим отображением в frontend/src/app/features/comments/components/comment-list.component.ts
- [ ] T328 [P] [EXT] Создать CommentItemComponent (Dumb) с avatar, timestamp в frontend/src/app/features/comments/components/comment-item.component.ts
- [ ] T329 [P] [EXT] Создать CommentFormComponent (Dumb) с поддержкой @mentions в frontend/src/app/features/comments/components/comment-form.component.ts
- [ ] T330 [P] [EXT] Создать MentionAutocompleteComponent (Dumb) в frontend/src/app/features/comments/components/mention-autocomplete.component.ts

### Frontend: Comments Containers

- [ ] T331 [EXT] Создать CommentsContainerComponent (Smart) в frontend/src/app/features/comments/containers/comments-container.component.ts
- [ ] T332 [EXT] Создать CommentsModule в frontend/src/app/features/comments/comments.module.ts

### Frontend: Tags Models & Store

- [ ] T333 [P] [EXT] Создать Tag model interface в frontend/src/app/shared/models/tag.model.ts
- [ ] T334 [EXT] Создать tags feature store (actions, reducer, effects, selectors) в frontend/src/app/features/tags/store/
- [ ] T335 [P] [EXT] Создать TagsApiService в frontend/src/app/features/tags/services/tags-api.service.ts
- [ ] T336 [EXT] Создать TagsFacade в frontend/src/app/features/tags/services/tags-facade.service.ts

### Frontend: Tags Components

- [ ] T337 [P] [EXT] Создать TagBadgeComponent (Dumb) для отображения метки в frontend/src/app/shared/components/tag-badge.component.ts
- [ ] T338 [P] [EXT] Создать TagSelectorComponent (Dumb) с autocomplete и color picker в frontend/src/app/features/tags/components/tag-selector.component.ts
- [ ] T339 [P] [EXT] Создать TagFormComponent (Dumb) для создания новой метки в frontend/src/app/features/tags/components/tag-form.component.ts
- [ ] T340 [P] [EXT] Создать TaskTagsListComponent (Dumb) в frontend/src/app/features/tasks/components/task-tags-list.component.ts

### Frontend: Tags Module

- [ ] T341 [EXT] Создать TagsModule в frontend/src/app/features/tags/tags.module.ts

### Frontend: Files Models & Services

- [ ] T342 [P] [EXT] Создать File model interface в frontend/src/app/shared/models/file.model.ts
- [ ] T343 [P] [EXT] Создать FilesApiService с upload/download методами в frontend/src/app/core/services/files-api.service.ts
- [ ] T344 [EXT] Создать FileUploadService для обработки drag & drop и progress в frontend/src/app/core/services/file-upload.service.ts

### Frontend: Files Components

- [ ] T345 [P] [EXT] Создать FileUploadComponent (Dumb) с drag & drop zone в frontend/src/app/shared/components/file-upload.component.ts
- [ ] T346 [P] [EXT] Создать FileListComponent (Dumb) в frontend/src/app/shared/components/file-list.component.ts
- [ ] T347 [P] [EXT] Создать FilePreviewComponent (Dumb) для изображений в frontend/src/app/shared/components/file-preview.component.ts
- [ ] T348 [P] [EXT] Создать FileProgressComponent (Dumb) для отображения загрузки в frontend/src/app/shared/components/file-progress.component.ts
- [ ] T349 [P] [EXT] Создать FileItemComponent (Dumb) с иконкой и действиями в frontend/src/app/shared/components/file-item.component.ts

### Testing: Comments

- [ ] T350 [EXT] Написать integration тест для CommentsController в backend/src/test/java/com/sprinto/tms/integration/CommentsControllerTest.java
- [ ] T351 [P] [EXT] Написать unit тест для CommentService в backend/src/test/java/com/sprinto/tms/unit/CommentServiceTest.java
- [ ] T352 [P] [EXT] Написать unit тест для MentionService в backend/src/test/java/com/sprinto/tms/unit/MentionServiceTest.java
- [ ] T353 [P] [EXT] Написать unit тест для CommentFormComponent в frontend/src/app/features/comments/components/comment-form.component.spec.ts
- [ ] T354 [P] [EXT] Написать unit тест для comments store reducer в frontend/src/app/features/comments/store/comments.reducer.spec.ts

### Testing: Tags

- [ ] T355 [EXT] Написать integration тест для TagsController в backend/src/test/java/com/sprinto/tms/integration/TagsControllerTest.java
- [ ] T356 [P] [EXT] Написать unit тест для TagService в backend/src/test/java/com/sprinto/tms/unit/TagServiceTest.java
- [ ] T357 [P] [EXT] Написать unit тест для TagSelectorComponent в frontend/src/app/features/tags/components/tag-selector.component.spec.ts
- [ ] T358 [P] [EXT] Написать unit тест для tags store в frontend/src/app/features/tags/store/tags.reducer.spec.ts

### Testing: Files

- [ ] T359 [EXT] Написать integration тест для FilesController в backend/src/test/java/com/sprinto/tms/integration/FilesControllerTest.java
- [ ] T360 [P] [EXT] Написать unit тест для FileService в backend/src/test/java/com/sprinto/tms/unit/FileServiceTest.java
- [ ] T361 [P] [EXT] Написать unit тест для FileStorageService в backend/src/test/java/com/sprinto/tms/unit/FileStorageServiceTest.java
- [ ] T362 [P] [EXT] Написать unit тест для FileQuotaService в backend/src/test/java/com/sprinto/tms/unit/FileQuotaServiceTest.java
- [ ] T363 [P] [EXT] Написать unit тест для FileUploadComponent в frontend/src/app/shared/components/file-upload.component.spec.ts
- [ ] T364 [P] [EXT] Написать unit тест для FileUploadService в frontend/src/app/core/services/file-upload.service.spec.ts

---

## Phase 12: Polish & Cross-Cutting Concerns

**Цель**: Завершить кроссфункциональные требования, оптимизацию, документацию.

**Критерии завершения**:
- ✅ Все linter ошибки исправлены
- ✅ Code coverage ≥80% для backend и ≥70% для frontend
- ✅ Performance optimization применена (кэширование, индексы, lazy loading)
- ✅ OpenAPI документация актуальна
- ✅ README и quickstart.md обновлены
- ✅ E2E тесты для критических flows проходят

### Code Quality & Testing

- [ ] T261 Запустить backend linter и исправить ошибки (Checkstyle/SpotBugs)
- [ ] T262 [P] Запустить frontend linter и исправить ошибки (ESLint)
- [ ] T263 Проверить backend code coverage и добавить тесты до ≥80%
- [ ] T264 [P] Проверить frontend code coverage и добавить тесты до ≥70%
- [ ] T265 Написать E2E тест для US1: создание задачи → таймер → завершение в frontend/tests/e2e/user-story-1.spec.ts
- [ ] T266 [P] Написать E2E тест для US2: создание проекта → добавление участников в frontend/tests/e2e/user-story-2.spec.ts
- [ ] T267 [P] Написать E2E тест для US4: генерация отчета → экспорт Excel в frontend/tests/e2e/user-story-4.spec.ts

### Additional Unit Tests - Backend Services (Phase 3-4)

- [ ] T365 [P] Написать unit тест для ProjectService в backend/src/test/java/com/sprinto/tms/unit/ProjectServiceTest.java
- [ ] T366 [P] Написать unit тест для ProjectMemberService в backend/src/test/java/com/sprinto/tms/unit/ProjectMemberServiceTest.java
- [ ] T367 [P] Написать unit тест для ProjectPermissionEvaluator в backend/src/test/java/com/sprinto/tms/unit/ProjectPermissionEvaluatorTest.java

### Additional Unit Tests - Backend Services (Phase 5-7)

- [ ] T368 [P] Написать unit тест для DashboardService в backend/src/test/java/com/sprinto/tms/unit/DashboardServiceTest.java
- [ ] T369 [P] Написать unit тест для UserSettingsService в backend/src/test/java/com/sprinto/tms/unit/UserSettingsServiceTest.java
- [ ] T370 [P] Написать unit тест для ReportService в backend/src/test/java/com/sprinto/tms/unit/ReportServiceTest.java
- [ ] T371 [P] Написать unit тест для CsvExportService в backend/src/test/java/com/sprinto/tms/unit/CsvExportServiceTest.java
- [ ] T372 [P] Написать unit тест для NotificationRouter в backend/src/test/java/com/sprinto/tms/unit/NotificationRouterTest.java
- [ ] T373 [P] Написать unit тест для PushNotificationService в backend/src/test/java/com/sprinto/tms/unit/PushNotificationServiceTest.java

### Additional Unit Tests - Backend Services (Phase 8-11)

- [ ] T374 [P] Написать unit тест для CalendarSyncScheduler в backend/src/test/java/com/sprinto/tms/unit/CalendarSyncSchedulerTest.java
- [ ] T375 [P] Написать unit тест для CalendarEventMapper в backend/src/test/java/com/sprinto/tms/unit/CalendarEventMapperTest.java
- [ ] T376 [P] Написать unit тест для AnalyticsService в backend/src/test/java/com/sprinto/tms/unit/AnalyticsServiceTest.java
- [ ] T377 [P] Написать unit тест для LeaderboardService в backend/src/test/java/com/sprinto/tms/unit/LeaderboardServiceTest.java
- [ ] T378 [P] Написать unit тест для BurndownChartService в backend/src/test/java/com/sprinto/tms/unit/BurndownChartServiceTest.java
- [ ] T379 [P] Написать unit тест для ProductivityScoreService в backend/src/test/java/com/sprinto/tms/unit/ProductivityScoreServiceTest.java
- [ ] T380 [P] Написать unit тест для RatingService в backend/src/test/java/com/sprinto/tms/unit/RatingServiceTest.java
- [ ] T381 [P] Написать unit тест для TaskDependencyService в backend/src/test/java/com/sprinto/tms/unit/TaskDependencyServiceTest.java

### Additional Unit Tests - Frontend Components (Phase 3-4)

- [ ] T382 [P] Написать unit тест для ProjectFormComponent в frontend/src/app/features/projects/components/project-form.component.spec.ts
- [ ] T383 [P] Написать unit тест для ProjectMembersListComponent в frontend/src/app/features/projects/components/project-members-list.component.spec.ts
- [ ] T384 [P] Написать unit тест для projects store reducer в frontend/src/app/features/projects/store/projects.reducer.spec.ts
- [ ] T385 [P] Написать unit тест для projects store effects в frontend/src/app/features/projects/store/projects.effects.spec.ts

### Additional Unit Tests - Frontend Components (Phase 5-7)

- [ ] T386 [P] Написать unit тест для DashboardContainerComponent в frontend/src/app/features/dashboard/containers/dashboard-container.component.spec.ts
- [ ] T387 [P] Написать unit тест для CalendarComponent в frontend/src/app/features/dashboard/components/calendar.component.spec.ts
- [ ] T388 [P] Написать unit тест для HoursWorkedChartWidgetComponent в frontend/src/app/features/dashboard/components/widgets/hours-worked-chart-widget.component.spec.ts
- [ ] T389 [P] Написать unit тест для ReportFiltersComponent в frontend/src/app/features/reports/components/report-filters.component.spec.ts
- [ ] T390 [P] Написать unit тест для ProjectReportChartComponent в frontend/src/app/features/reports/components/project-report-chart.component.spec.ts
- [ ] T391 [P] Написать unit тест для reports store reducer в frontend/src/app/features/reports/store/reports.reducer.spec.ts
- [ ] T392 [P] Написать unit тест для NotificationDropdownComponent в frontend/src/app/features/notifications/components/notification-dropdown.component.spec.ts
- [ ] T393 [P] Написать unit тест для notifications store effects в frontend/src/app/features/notifications/store/notifications.effects.spec.ts

### Additional Unit Tests - Frontend Components (Phase 8-11)

- [ ] T394 [P] Написать unit тест для CalendarSelectorComponent в frontend/src/app/features/calendar/components/calendar-selector.component.spec.ts
- [ ] T395 [P] Написать unit тест для LeaderboardComponent в frontend/src/app/features/analytics/components/leaderboard.component.spec.ts
- [ ] T396 [P] Написать unit тест для BurndownChartComponent в frontend/src/app/features/analytics/components/burndown-chart.component.spec.ts
- [ ] T397 [P] Написать unit тест для ProductivityScoreDisplayComponent в frontend/src/app/features/rating/components/productivity-score-display.component.spec.ts
- [ ] T398 [P] Написать unit тест для TaskTreeViewComponent в frontend/src/app/features/tasks/components/task-tree-view.component.spec.ts

### Contract Tests

- [ ] T399 Настроить Spring Cloud Contract / Pact для contract testing в backend/src/test/java/com/sprinto/tms/contract/
- [ ] T400 Написать contract test для AuthController в backend/src/test/java/com/sprinto/tms/contract/AuthControllerContractTest.java
- [ ] T401 [P] Написать contract test для TaskController в backend/src/test/java/com/sprinto/tms/contract/TaskControllerContractTest.java
- [ ] T402 [P] Написать contract test для ProjectController в backend/src/test/java/com/sprinto/tms/contract/ProjectControllerContractTest.java
- [ ] T403 [P] Написать contract test для TimeTrackingController в backend/src/test/java/com/sprinto/tms/contract/TimeTrackingControllerContractTest.java
- [ ] T404 [P] Написать contract test для ReportsController в backend/src/test/java/com/sprinto/tms/contract/ReportsControllerContractTest.java
- [ ] T405 [P] Написать contract test для NotificationsController в backend/src/test/java/com/sprinto/tms/contract/NotificationsControllerContractTest.java

### Additional E2E Tests

- [ ] T406 [P] Написать E2E тест для US3: дашборд с виджетами в frontend/tests/e2e/user-story-3.spec.ts
- [ ] T407 [P] Написать E2E тест для US5: уведомления в frontend/tests/e2e/user-story-5.spec.ts
- [ ] T408 [P] Написать E2E тест для комментариев и упоминаний в frontend/tests/e2e/comments-mentions.spec.ts
- [ ] T409 [P] Написать E2E тест для меток в frontend/tests/e2e/tags.spec.ts
- [ ] T410 [P] Написать E2E тест для файловых вложений в frontend/tests/e2e/file-attachments.spec.ts

### Performance & Load Testing

- [ ] T635 Настроить инструмент нагрузочного тестирования backend (например, JMeter или Gatling) и базовый сценарий нагрузки для основных REST endpoints
- [ ] T636 [P] Разработать сценарии нагрузочного тестирования для таймеров и WebSocket‑уведомлений (SC-002, SC-005, SC-006) с измерением задержек и стабильности соединений
- [ ] T637 [P] Разработать сценарии нагрузочного тестирования генерации отчётов и экспорта (SC-004, SC-008) для типичных объёмов данных
- [ ] T638 [P] Настроить измерение производительности фронтенда (FPS, время рендеринга drag & drop списков) для проверки SC-015, используя E2E/perf‑инструменты
- [ ] T639 Интегрировать перфоманс/нагрузочные тесты в CI/CD pipeline с отчётами по ключевым SC‑метрикам

### Product Analytics & Usage Metrics (SC-012–SC-014)

- [ ] T650 Спроектировать таксономию и схему событий продуктовой аналитики (использование отчётности и дашбордов, успешное выполнение P1-сценариев, обращения в поддержку) в соответствии с NFR-050–NFR-052
- [ ] T651 [P] Реализовать логирование событий использования отчётности и BI-дашбордов на backend (генерация отчётов, просмотр дашбордов, экспорт) с агрегацией по SC-012
- [ ] T652 [P] Реализовать логирование ключевых событий онбординга и завершения P1-сценариев (US1/US2) на frontend/backend для оценки SC-013
- [ ] T653 [P] Настроить хранилище/интеграцию для агрегирования метрик SC-012–SC-014 (внутренняя БД или внешняя аналитическая система) и периодическое построение сводных метрик
- [ ] T654 [P] Создать административный или BI-дашборд для просмотра агрегированных метрик SC-012–SC-014 в frontend/src/app/features/analytics/components/product-analytics-dashboard.component.ts
- [ ] T655 [P] Написать E2E/интеграционные тесты, проверяющие генерацию ключевых событий аналитики для типичных пользовательских сценариев в frontend/tests/e2e/product-analytics.spec.ts

### Performance Optimization

- [ ] T268 Реализовать кэширование в backend (Spring Cache + Caffeine) для user permissions, project members
- [ ] T269 [P] Настроить connection pool sizing для R2DBC в backend/src/main/resources/application.yml
- [ ] T270 Добавить database индексы для часто запрашиваемых полей (если не добавлены в миграциях)
- [ ] T271 [P] Реализовать pagination для списков задач, отчетов в TaskController и ReportsController
- [ ] T272 [P] Настроить Angular lazy loading для всех feature modules в frontend/src/app/app-routing.module.ts
- [ ] T273 [P] Применить Angular CDK Virtual Scrolling для длинных списков задач в TaskListContainerComponent
- [ ] T274 [P] Настроить Tailwind CSS purge для удаления неиспользуемых классов в frontend/tailwind.config.js
- [ ] T275 Проверить bundle size frontend (<2MB gzipped) и оптимизировать при необходимости

### Change Detection & Long Lists (NFR-013, NFR-017, NFR-018, SC-002, SC-004, SC-008, SC-015)

- [ ] T711 [P] Провести аудит всех Presentational компонентов frontend и убедиться, что для них установлен ChangeDetectionStrategy.OnPush и используются иммутабельные @Input значения (за исключением явно обоснованных случаев) (см. NFR-018)
- [ ] T712 [P] Добавить линтер/архитектурное правило (ESLint/ts‑rules или custom schematic), предотвращающее использование Default change detection в Presentational компонентах (см. NFR-018)
- [ ] T713 [P] Применить Angular CDK Virtual Scrolling или эффективную пагинацию ко всем длинным спискам, кроме TaskList (уведомления, отчёты, leaderboard и др.), для выполнения NFR-017 и SC-015 (см. NFR-013, NFR-017, SC-004, SC-008, SC-015)

### Security & Validation

- [ ] T276 Добавить Bean Validation (@Valid) на всех DTO endpoints в backend controllers
- [ ] T277 [P] Настроить CORS в backend SecurityConfig с правильными allowed origins
- [ ] T278 [P] Реализовать rate limiting с Bucket4j для критичных endpoints в backend
- [ ] T279 [P] Настроить HTTPS и secure headers (CSP, HSTS) в backend SecurityConfig
- [ ] T280 Реализовать audit logging для security events (failed logins, permission denials)

### Documentation

- [ ] T281 [P] Обновить OpenAPI спецификацию в specs/001-team-task-manager/contracts/openapi.yaml с актуальными endpoints
- [ ] T282 [P] Обновить WebSocket протокол документацию в specs/001-team-task-manager/contracts/websocket.md
- [ ] T283 Обновить README.md в корне backend/ проекта с инструкциями по запуску
- [ ] T284 [P] Обновить README.md в корне frontend/ проекта с инструкциями по development
- [ ] T285 Обновить quickstart.md в specs/001-team-task-manager/ с актуальными шагами setup
- [ ] T286 [P] Создать API documentation через Swagger UI endpoint в backend (/swagger-ui.html)

### Deployment & CI/CD

- [ ] T287 Создать Dockerfile для backend в backend/Dockerfile
- [ ] T288 [P] Создать Dockerfile для frontend в frontend/Dockerfile
- [ ] T289 Создать docker-compose.yml для production deployment в корне репозитория
- [ ] T290 [P] Настроить GitHub Actions / GitLab CI pipeline (.github/workflows/ci.yml или .gitlab-ci.yml)
- [ ] T291 Настроить environment variables для production в backend/src/main/resources/application-prod.yml

### CI Quality Gates

- [ ] T656 Настроить в CI/CD pipeline обязательные шаги запуска unit, integration, contract и E2E тестов для backend и frontend с прерыванием pipeline при любой ошибке
- [ ] T657 [P] Настроить проверки порогов code coverage (≥80% backend, ≥70% frontend по NFR-028/NFR-029) в CI/CD pipeline и блокировку merge при снижении покрытия
- [ ] T658 [P] Интегрировать сценарии Performance & Load Testing (T635–T639) в CI/CD pipeline с блокировкой merge при нарушении критичных перфоманс-метрик (SC-002, SC-004, SC-005, SC-006, SC-008, SC-015)
- [ ] T659 Обновить документацию по процессу разработки (README/CONTRIBUTING) с описанием quality gates и требований к успешному merge в основную ветку

### Constitution Compliance (Constitution §§Core Principles, Testing Strategy, Performance & Scalability)

- [ ] T717 Создать или обновить шаблон Pull Request и CONTRIBUTING.md, добавив явный раздел "Constitution compliance checklist" (Reactive-First, Security-First, Testing Strategy, Performance & Scalability) для авторов PR (см. Constitution §§I, III, VI, VII; NFR-001–NFR-003, NFR-020–NFR-026, NFR-028–NFR-037.2)
- [ ] T718 [P] Обновить чеклисты code review (requirements.md и другие внутренние документы), включив обязательную проверку соответствия конституции и ключевым NFR при ревью изменений (см. Constitution §Governance, §Testing Strategy, NFR-028–NFR-037.2)

### User Experience

- [ ] T292 [P] Добавить loading indicators для всех async операций в frontend
- [ ] T293 [P] Добавить error messages для всех форм в frontend (validation errors, API errors)
- [ ] T294 [P] Реализовать toast notifications для success/error feedback в frontend
- [ ] T295 [P] Добавить confirmation dialogs для destructive actions (удаление проекта, задачи) в frontend
- [ ] T296 Проверить responsive design на мобильных устройствах и планшетах
- [ ] T297 [P] Добавить keyboard shortcuts для часто используемых действий (создать задачу, запустить таймер)

---

## Зависимости между User Stories

### Граф зависимостей

```
Phase 1 (Setup) → Phase 2 (Foundational)
                        ↓
                  Phase 3 (US1) ────┐
                        ↓           │
                  Phase 4 (US2) ────┤
                        ↓           │
        ┌───────────────┼───────────┴────────┐
        ↓               ↓                    ↓
   Phase 5 (US3)   Phase 6 (US4)      Phase 7 (US5)
        │               │                    │
        └───────────────┼────────────────────┘
                        ↓
        ┌───────────────┼───────────────┐
        ↓               ↓               ↓
   Phase 8 (US6)   Phase 9 (US7)  Phase 10 (US8)  Phase 11 (US9)
        │               │               │               │
        └───────────────┴───────────────┴───────────────┘
                                ↓
                        Phase 12 (Polish)
```

### Критические зависимости

**Блокирующие**:
- **US1 блокирует все остальные**: Базовое управление задачами необходимо для всех остальных функций
- **US2 блокирует US3-US9**: Проекты и команда необходимы для дашбордов, отчетов, уведомлений

**Рекомендуемые**:
- US1 + US2 → US3 (Дашборд использует данные задач и проектов)
- US1 + US2 → US4 (Отчеты требуют задачи и проекты)
- US1 + US2 → US5 (Уведомления о задачах и проектах)

**Независимые** (можно делать параллельно после US1+US2):
- US3, US4, US5 (P2) - могут выполняться параллельно разными командами
- US6, US7, US8, US9 (P3) - независимы друг от друга

---

## Параллельное выполнение: Примеры команд

### Команда 1 (Backend Focus)
**Phase 3-4**: Backend-задачи по US1/US2 (Task/TimeEntry/Project сервисы, репозитории, контроллеры, WebSocket, безопасность)
**Phase 5-7**: Backend-задачи по дашбордам, отчётам и уведомлениям (Dashboard/Report/Notification сервисы и контроллеры)

### Команда 2 (Frontend Focus)
**Phase 3-4**: Frontend-задачи по US1/US2 (tasks/time-tracking/projects модули, smart/presentational компоненты, NgRx store)
**Phase 5-7**: Frontend-задачи по дашбордам, отчётам и уведомлениям (dashboard/reports/notifications модули, графики, E2E)

### Команда 3 (Features P3)
**Phase 8-11**: US6–US9 (интеграция с календарём, BI-дашборды, рейтинги, иерархия задач) — любая согласованная последовательность

---

## Отслеживание прогресса

### Статус по фазам

- [ ] Phase 1: Setup (6 задач)
- [ ] Phase 2: Foundational (39 задач) - добавлено 13 тестовых и инфраструктурных задач
- [ ] Phase 3: User Story 1 (56 задач) - добавлено 10 тестовых и UX задач
- [ ] Phase 4: User Story 2 (38 задач)
- [ ] Phase 4.5: Extended Features - Комментарии, Метки, Файлы (67 задач) - НОВАЯ ФАЗА
- [ ] Phase 5: User Story 3 (23 задачи)
- [ ] Phase 6: User Story 4 (25 задач)
- [ ] Phase 7: User Story 5 (30 задач)
- [ ] Phase 8: User Story 6 (25 задач)
- [ ] Phase 9: User Story 7 (20 задач)
- [ ] Phase 10: User Story 8 (13 задач)
- [ ] Phase 11: User Story 9 (21 задача)
- [ ] Phase 12: Polish & Testing (192 задач) - добавлено 155 тестовых и кросс-функциональных задач

### MVP Checkpoint

После завершения **Phase 1-3** (US1) у вас будет работающий MVP:
- ✅ Регистрация и аутентификация
- ✅ Создание задач
- ✅ Учет времени через таймеры
- ✅ Real-time обновления через WebSocket
- ✅ Изменение статусов

**Рекомендация**: Развернуть MVP в production после Phase 3 для получения обратной связи от пользователей.

---

## Резюме

**Всего задач**: 490  
**Разбивка по категориям (приблизительно)**:
- Функциональные задачи (implementation)
- Тестовые задачи (unit + integration + contract + E2E)
- Infrastructure & Setup / Cross-cutting (инфраструктура, CI/CD, производительность, аналитика и т.п.)

**Дополнительные функции** (добавлено):
- Password Recovery flow (FR-007.1): 8 задач (T537-T544)
- Time Entry Audit Trail (FR-056-059): 9 задач (T545-T553)

**Оценка времени** (rough estimate):
- Phase 1-2: 2-4 недели (1 разработчик full-time)
- Phase 3-4 (P1): 6-8 недель (команда 2-3 разработчика) - включая Audit Trail
- Phase 4.5 (Extended Features): 3-4 недели (комментарии, метки, файлы)
- Phase 5-7 (P2): 5-6 недель
- Phase 8-11 (P3): 4-5 недель
- Phase 12 (Polish & Testing): 4-5 недель

**Итого**: ~24-32 недели для полной реализации всех user stories с полным покрытием тестами

**MVP (Phase 1-3)**: ~8-12 недель - включая Password Recovery и Audit Trail

**Extended MVP (Phase 1-4.5)**: ~14-19 недель - включает комментарии, метки и файлы

---

**Готовность к реализации**: ✅ Да  
**Дата создания**: 2025-11-14  
**Следующий шаг**: Начать с Phase 1 (Setup) → T001

