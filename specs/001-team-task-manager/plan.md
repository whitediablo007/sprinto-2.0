# План реализации: Система управления проектами и задачами для малых команд

**Ветка**: `001-team-task-manager` | **Дата**: 2025-11-14 | **Спецификация**: [spec.md](./spec.md)  
**Входные данные**: Спецификация фичи из `/specs/001-team-task-manager/spec.md`

## Краткое содержание

Реализация Single Page Application (SPA) для управления проектами и задачами в малых командах с реактивным backend на Spring Boot + Project Reactor и современным frontend на Angular 18+. Система обеспечивает: учет рабочего времени через таймеры с WebSocket обновлениями, планирование ресурсов с календарем, формирование отчетов с интерактивной аналитикой, BI дашборды для оценки эффективности, систему рейтинга команды, интеграцию с Google Calendar через OAuth 2.0 и уведомления в реальном времени.

**Технический подход**: Модульный монолит (Modular Monolith) с реактивным backend (R2DBC для неблокирующего доступа к данным), чёткими доменными границами между модулями, Event-Driven коммуникация через WebSocket для real-time функций, JWT-based аутентификация с Spring Security, NgRx для state management на frontend, OpenAPI для документирования контрактов API.

## Технический контекст

**Язык/Версия**: 
- Backend: Java 17+ (LTS)
- Frontend: TypeScript 5.0+

**Основные зависимости**:
- **Backend**: Spring Boot 3.x, Spring WebFlux (Project Reactor), Spring Security 6.x, Spring Data R2DBC, SpringDoc OpenAPI 2.x, PostgreSQL R2DBC Driver, Flyway (для миграций), Lombok, MapStruct (для маппинга DTO)
- **Frontend**: Angular 18+, PrimeNG 17+, Tailwind CSS 3.x, NgRx 18+ (Store, Effects, Entity), RxJS 7+, Angular CDK 18+, FullCalendar Angular, Chart.js/NGX-Charts для аналитики

**Хранилище данных**: PostgreSQL 15+ с R2DBC для реактивного доступа

**Тестирование**:
- Backend: JUnit 5, Spring Boot Test, Reactor Test (StepVerifier), Testcontainers для интеграционных тестов, WireMock для mock внешних API
- Frontend: Jasmine, Karma, Cypress для E2E тестирования, NgRx Testing utilities

**Целевая платформа**: Web (SPA), адаптивный дизайн для десктопов, планшетов и мобильных устройств

**Тип проекта**: Web application (frontend + backend)

**Цели производительности**:
- Обновление активного таймера в реальном времени с задержкой ≤1 секунда
- Генерация отчета по проекту (100+ задач) ≤5 секунд
- Поддержка 100 одновременных пользователей без деградации
- Доставка WebSocket уведомлений ≤2 секунды
- UI операции (drag & drop) с частотой ≥30 FPS

**Ограничения**:
- p95 latency для API endpoints ≤200ms
- Размер bundle frontend (initial load) ≤2MB (gzipped)
- Использование памяти backend ≤512MB на 100 пользователей
- WebSocket соединения: поддержка до 500 одновременных подключений

**Масштаб/Область**:
- Целевая аудитория: команды 5-15 человек
- Проектов на команду: 5-20 активных
- Задач на проект: 50-200 активных
- Записей времени: ~1000 в месяц на команду
- Файлов: макс 10MB на файл, до 100 файлов на проект

## Проверка Constitution

*GATE: Должна пройти перед Phase 0 исследования. Повторная проверка после Phase 1 проектирования.*

**Статус**: ✅ **СООТВЕТСТВУЕТ** - План полностью соответствует Constitution v1.0.0

**Constitution**: `.specify/memory/constitution.md` (версия 1.0.0, ратифицирована 2025-11-14)

### Проверка соответствия основным принципам:

#### I. Reactive-First Architecture (NON-NEGOTIABLE) ✅
- ✅ Backend использует Spring WebFlux и Project Reactor (Mono<T>, Flux<T>)
- ✅ R2DBC для неблокирующего доступа к PostgreSQL
- ✅ WebSocket с реактивными потоками для backpressure
- ✅ Блокирующие операции (Flyway, file I/O) изолируются на boundedElastic scheduler
- **Обоснование**: Критично для real-time функций (таймеры с latency ≤1с, WebSocket ≤2с, поддержка 100 concurrent users)

#### II. Modular Monolith Architecture ✅
- ✅ Архитектура организована как модульный монолит с доменными границами
- ✅ Домены: User & Auth, Project, Task, Time Tracking, Notification, Reporting, Integration
- ✅ Каждый модуль имеет: собственный package, domain entities, repositories, services
- ✅ Кросс-модульная коммуникация через явные интерфейсы сервисов
- ✅ Frontend использует lazy-loaded feature modules с изолированным state
- **Преимущества**: Простота deployment + масштабируемость кодовой базы + путь к микросервисам при необходимости

#### III. Security-First ✅
- ✅ JWT токены (access 15 мин, refresh 7 дней) - NFR-020
- ✅ BCrypt с cost factor 12 - NFR-021
- ✅ Bean Validation (JSR-380) на всех DTO - NFR-022
- ✅ @ControllerAdvice для централизованной обработки исключений - NFR-023
- ✅ @PreAuthorize для авторизации - NFR-024
- ✅ HTTPS + security headers в production - NFR-025
- ✅ Rate limiting на критичных endpoints - NFR-026

#### IV. Constructor Injection (Backend) ✅
- ✅ Использование @RequiredArgsConstructor (Lombok)
- ✅ Field injection запрещён
- ✅ Constructor parameters как private final fields

#### V. Smart/Presentational Component Separation (Frontend) ✅
- ✅ Smart components в `features/{feature}/containers/` с NgRx Store
- ✅ Presentational components в `features/{feature}/components/` с @Input/@Output
- ✅ OnPush change detection для всех Presentational компонентов
- ✅ Использование `inject()` function для DI в новых компонентах

#### VI. Testing Strategy (NON-NEGOTIABLE) ⚠️
- ✅ Backend: JUnit 5 + Mockito для unit tests
- ✅ Spring Boot Test + WebFlux Test для integration tests
- ✅ Testcontainers для PostgreSQL в integration tests
- ✅ WebSocket handlers testing с WebSocketClient
- ✅ Contract tests для OpenAPI validation
- ✅ Frontend: Jasmine + Karma для unit tests
- ✅ NgRx store tests (reducers, effects, selectors)
- ✅ E2E тесты (Cypress) для критичных user flows
- ⚠️ **ВНИМАНИЕ**: tasks.md содержит недостаточно тестовых задач для достижения ≥80% backend / ≥70% frontend coverage
- **Требуется**: Добавить ~60-80 тестовых задач в tasks.md

#### VII. Performance & Scalability ✅
- ✅ API p95 latency ≤200ms - NFR-008
- ✅ Поддержка 100 concurrent users - NFR-010
- ✅ Timer updates ≤1s latency - NFR-011
- ✅ Report generation ≤5s для 100+ задач - NFR-012
- ✅ Frontend bundle ≤2MB gzipped - NFR-013
- ✅ Drag & drop ≥30 FPS - NFR-014
- ✅ Database indexes для частых запросов - NFR-015
- ✅ Virtual scrolling для списков >100 items - NFR-017
- ✅ Кэширование (Spring Cache + Caffeine) для read-heavy data - NFR-019

### Backend Best Practices ✅
- ✅ Gradle с Kotlin DSL (build.gradle.kts)
- ✅ Централизованная обработка исключений (@ControllerAdvice)
- ✅ SLF4J с Logback (INFO для бизнес-операций, DEBUG для development)
- ✅ Bean Validation (JSR-380) на всех DTO
- ✅ MapStruct для DTO ↔ Entity маппинга

### Frontend Best Practices ✅
- ✅ NgRx для глобального state + Signals для локального
- ✅ inject() function для DI
- ✅ OnPush change detection для Presentational компонентов
- ✅ Reactive Forms (Template-driven запрещены)
- ✅ trackBy для всех *ngFor
- ✅ async pipe для observables

### Обнаруженные риски:

1. **⚠️ MEDIUM**: Недостаточное покрытие тестами в tasks.md
   - **Действие**: Добавить тестовые задачи для достижения требуемого coverage
   
2. **✅ RESOLVED**: Противоречие "микросервисы" vs "modular monolith"
   - **Действие**: Исправлено в этом обновлении

**Вывод**: План **СООТВЕТСТВУЕТ** всем принципам Constitution v1.0.0. Требуется только дополнить tasks.md тестовыми задачами для полного соответствия Principle VI.

## Структура проекта

### Документация (эта фича)

```text
specs/001-team-task-manager/
├── spec.md                 # Спецификация фичи
├── plan.md                 # Этот файл (вывод /speckit.plan)
├── research.md             # Phase 0 - исследование и решения
├── data-model.md           # Phase 1 - модель данных
├── quickstart.md           # Phase 1 - руководство по быстрому старту
├── contracts/              # Phase 1 - контракты API
│   ├── openapi.yaml        # OpenAPI спецификация
│   └── websocket.md        # Протокол WebSocket
├── checklists/             # Чеклисты валидации
│   └── requirements.md     # Чеклист требований спецификации
└── tasks.md                # Phase 2 (создается командой /speckit.tasks)
```

### Исходный код (корень репозитория)

```text
backend/
├── src/
│   ├── main/
│   │   ├── java/com/sprinto/tms/
│   │   │   ├── config/              # Конфигурация Spring
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── R2dbcConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── domain/              # Доменные сущности
│   │   │   │   ├── user/
│   │   │   │   ├── project/
│   │   │   │   ├── task/
│   │   │   │   ├── timeentry/
│   │   │   │   └── notification/
│   │   │   ├── repository/          # R2DBC репозитории
│   │   │   ├── service/             # Бизнес-логика
│   │   │   ├── api/                 # REST контроллеры
│   │   │   │   ├── rest/            # HTTP endpoints
│   │   │   │   └── websocket/       # WebSocket handlers
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   ├── mapper/              # MapStruct mappers
│   │   │   ├── security/            # Security: JWT, filters
│   │   │   ├── exception/           # Exception handling
│   │   │   └── util/                # Утилиты
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/        # Flyway миграции
│   │           ├── V1__initial_schema.sql
│   │           ├── V2__add_indexes.sql
│   │           └── ...
│   └── test/
│       ├── java/com/sprinto/tms/
│       │   ├── integration/         # Интеграционные тесты
│       │   ├── unit/                # Unit тесты
│       │   └── contract/            # Contract тесты
│       └── resources/
│           └── application-test.yml
├── build.gradle.kts          # Gradle Kotlin DSL
├── settings.gradle.kts
└── README.md

frontend/
├── src/
│   ├── app/
│   │   ├── core/                    # Singleton сервисы, guards, interceptors
│   │   │   ├── auth/
│   │   │   ├── guards/
│   │   │   ├── interceptors/
│   │   │   └── services/
│   │   ├── shared/                  # Shared компоненты, directives, pipes
│   │   │   ├── components/
│   │   │   ├── directives/
│   │   │   ├── pipes/
│   │   │   └── models/
│   │   ├── features/                # Feature modules
│   │   │   ├── auth/
│   │   │   │   ├── containers/      # Smart components (Store + Logic)
│   │   │   │   ├── components/      # Dumb components (@Input/@Output)
│   │   │   │   ├── store/           # NgRx: actions, reducers, effects, selectors
│   │   │   │   └── auth.module.ts
│   │   │   ├── dashboard/
│   │   │   │   ├── containers/
│   │   │   │   ├── components/
│   │   │   │   └── store/
│   │   │   ├── projects/
│   │   │   │   ├── containers/
│   │   │   │   ├── components/
│   │   │   │   └── store/
│   │   │   ├── tasks/
│   │   │   │   ├── containers/      # task-list-container, task-details-container
│   │   │   │   ├── components/      # task-card, task-form, task-filters
│   │   │   │   └── store/
│   │   │   ├── time-tracking/
│   │   │   │   ├── containers/
│   │   │   │   ├── components/      # timer-display, time-entry-form
│   │   │   │   └── store/
│   │   │   ├── reports/
│   │   │   ├── calendar/
│   │   │   ├── notifications/
│   │   │   └── settings/
│   │   ├── store/                   # Root store
│   │   │   ├── root-state.ts
│   │   │   └── root-reducers.ts
│   │   ├── app-routing.module.ts
│   │   ├── app.component.ts
│   │   └── app.module.ts
│   ├── assets/
│   │   ├── images/
│   │   ├── icons/
│   │   └── i18n/
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   ├── styles/
│   │   ├── tailwind.css
│   │   ├── primeng-theme.scss
│   │   └── _variables.scss
│   ├── index.html
│   ├── main.ts
│   └── styles.scss
├── tests/
│   ├── unit/                        # Jasmine/Karma тесты
│   ├── integration/
│   └── e2e/                         # Cypress тесты
│       ├── integration/
│       └── support/
├── angular.json
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── README.md

shared/                              # Общие артефакты (опционально)
└── api-contracts/
    └── openapi.yaml
```

**Решение по структуре**: Выбрана структура Web application (Option 2) с разделением на `backend/` и `frontend/`. Backend организован по domain-driven подходу с реактивными репозиториями. Frontend следует Angular best practices с feature modules и NgRx для state management. Flyway миграции обеспечат версионирование схемы БД.

## Отслеживание сложности

> **Заполняется ТОЛЬКО если Constitution Check имеет нарушения, требующие обоснования**

**Статус**: ✅ **НЕТ НАРУШЕНИЙ** - План полностью соответствует Constitution v1.0.0

**Обоснование технических решений**:

1. **Реактивное программирование (Spring WebFlux + Project Reactor)**
   - **Сложность**: Высокая (крутая кривая обучения, нетривиальная отладка)
   - **Обоснование**: ОБЯЗАТЕЛЬНО согласно Constitution Principle I (NON-NEGOTIABLE)
   - **Необходимость**: Критично для достижения success criteria:
     - SC-002: Timer updates с latency ≤1 секунда
     - SC-005: Поддержка 100 concurrent users без деградации
     - SC-006: WebSocket уведомления ≤2 секунды
   - **Альтернатива**: Блокирующий I/O (Spring MVC + JDBC) НЕ обеспечит требуемую производительность
   
2. **Модульный монолит вместо микросервисов**
   - **Решение**: Modular Monolith с чёткими доменными границами
   - **Обоснование**: Балансирует простоту deployment и масштабируемость кодовой базы
   - **Преимущества**: 
     - Единый deployment artifact (проще CI/CD)
     - Отсутствие сетевых вызовов между модулями (выше производительность)
     - Путь к микросервисам при необходимости (чёткие границы)
   - **Соответствие**: Constitution Principle II
   
3. **NgRx для state management**
   - **Сложность**: Средняя (boilerplate code, обучение концепций)
   - **Обоснование**: Constitution Principle V + необходимость управления сложным state
   - **Необходимость**: Критично для real-time синхронизации (WebSocket updates, optimistic UI)
   - **Митигация**: Facade services скрывают NgRx complexity от компонентов

**Вывод**: Все технические решения с повышенной сложностью либо обязательны по Constitution (Reactive), либо являются best practices для поставленных задач (NgRx). Альтернативы с меньшей сложностью не обеспечат требуемые характеристики системы.
