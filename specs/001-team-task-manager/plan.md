# План реализации: Система управления проектами и задачами для малых команд

**Ветка**: `001-team-task-manager` | **Дата**: 2025-11-14 | **Спецификация**: [spec.md](./spec.md)  
**Входные данные**: Спецификация фичи из `/specs/001-team-task-manager/spec.md`

## Краткое содержание

Реализация Single Page Application (SPA) для управления проектами и задачами в малых командах с реактивным backend на Spring Boot + Project Reactor и современным frontend на Angular 18+. Система обеспечивает: учет рабочего времени через таймеры с WebSocket обновлениями, планирование ресурсов с календарем, формирование отчетов с интерактивной аналитикой, BI дашборды для оценки эффективности, систему рейтинга команды, интеграцию с Google Calendar через OAuth 2.0 и уведомления в реальном времени.

**Технический подход**: Микросервисная архитектура с реактивным backend (R2DBC для неблокирующего доступа к данным), Event-Driven коммуникация через WebSocket для real-time функций, JWT-based аутентификация с Spring Security, NgRx для state management на frontend, OpenAPI для документирования контрактов API.

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

**Статус**: ⚠️ Constitution файл не заполнен, применяем базовые принципы для проекта

### Базовые архитектурные принципы для данного проекта:

1. **Реактивное программирование**: Использование Project Reactor для неблокирующих операций, особенно критично для real-time функций (таймеры, WebSocket)
2. **Разделение ответственности**: Четкое разделение backend (бизнес-логика, данные) и frontend (UI, UX)
3. **API-First подход**: Контракты API документируются через OpenAPI перед реализацией
4. **State management**: Централизованное управление состоянием на frontend через NgRx
5. **Тестируемость**: Модульные тесты для бизнес-логики, интеграционные для API, E2E для критических user flows

### Проверки:

✅ **Подходящий стек**: Spring WebFlux + R2DBC + Angular подходит для SPA с real-time требованиями  
✅ **Масштабируемость**: Реактивный подход обеспечит поддержку целевой нагрузки (100 одновременных пользователей)  
✅ **Разделение ответственности**: Backend и frontend разделены, коммуникация через REST API + WebSocket  
✅ **Тестируемость**: Предусмотрены тестовые фреймворки для всех слоев  
⚠️ **Сложность стека**: Реактивное программирование имеет крутую кривую обучения - оправдано real-time требованиями

**Вывод**: План соответствует базовым принципам. Сложность стека оправдана функциональными требованиями (WebSocket, высокая производительность).

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

**Статус**: Нет критических нарушений, требующих обоснования.

**Замечание**: Сложность реактивного стека (Spring WebFlux + Project Reactor) обоснована требованиями к real-time обновлениям (таймеры, WebSocket уведомления) и целями производительности (100 одновременных пользователей). Альтернатива с блокирующим I/O (Spring MVC + JDBC) не обеспечит требуемую производительность при заданной нагрузке.
