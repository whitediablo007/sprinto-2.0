<!--
Sync Impact Report:
Version Change: (none) → 1.0.0 (Initial ratification)
Modified Principles: N/A (new constitution)
Added Sections:
  - Core Principles (7 principles)
  - Backend Best Practices
  - Frontend Best Practices
  - Governance
Templates Requiring Updates:
  ✅ plan-template.md - Constitution Check section aligns with principles
  ✅ spec-template.md - Requirements align with architecture principles
  ✅ tasks-template.md - Task categorization reflects testing and quality principles
Follow-up TODOs: None
-->

# Team Task Manager Constitution

## Core Principles

### I. Reactive-First Architecture (NON-NEGOTIABLE)

The system MUST use reactive programming patterns throughout the entire stack to ensure non-blocking I/O and efficient resource utilization.

**Mandatory Requirements:**
- Backend MUST use Spring WebFlux with Project Reactor (Mono, Flux)
- Database access MUST use R2DBC for non-blocking queries
- All service methods MUST return reactive types (Mono<T>, Flux<T>)
- Blocking operations (Flyway, file I/O) MUST be isolated on boundedElastic scheduler
- WebSocket MUST leverage reactive streams for backpressure handling

**Rationale:** Required to support 100+ concurrent users with real-time features (timers, notifications) without resource exhaustion. Blocking I/O patterns would create performance bottlenecks and fail to meet SC-002 (timer latency <1s) and SC-006 (WebSocket <2s) success criteria.

### II. Modular Monolith Architecture

The application MUST be structured as a modular monolith with clear domain boundaries and potential for future extraction into microservices.

**Mandatory Requirements:**
- Backend MUST organize code into domain modules: User & Auth, Project, Task, Time Tracking, Notification, Reporting, Integration
- Each module MUST have clear boundaries: own package, domain entities, repositories, services
- Cross-module communication MUST occur through well-defined service interfaces
- Shared concerns (security, validation, exceptions) MUST reside in common packages
- Frontend MUST use lazy-loaded feature modules with isolated state

**Rationale:** Balances simplicity of deployment with scalability of codebase. Allows team to work on features independently and provides path to microservices if needed without big-bang rewrite.

### III. Security-First

Security MUST be built into every layer of the application, not bolted on afterwards.

**Mandatory Requirements:**
- Authentication MUST use JWT tokens with short-lived access tokens (15 min) and long-lived refresh tokens (7 days)
- Passwords MUST be hashed with BCrypt (cost factor 12 minimum)
- All DTO inputs MUST be validated with Bean Validation (JSR-380) using @Valid
- Centralized exception handling MUST use @ControllerAdvice to prevent information leakage
- Authorization MUST use @PreAuthorize with role/permission checks on all protected endpoints
- HTTPS MUST be enforced in production with secure headers (CSP, HSTS, X-Content-Type-Options)
- Rate limiting MUST be applied to authentication and critical endpoints

**Rationale:** Security breaches can destroy user trust and violate data protection regulations. Proactive security design prevents vulnerabilities that are expensive to fix later.

### IV. Constructor Injection (Backend)

All Spring components MUST use constructor injection for dependency management.

**Mandatory Requirements:**
- Use @RequiredArgsConstructor (Lombok) for constructor generation
- Field injection (@Autowired on fields) is PROHIBITED
- Setter injection MAY be used only for optional dependencies (explicitly justified)
- Constructor parameters MUST be declared as private final fields

**Rationale:** Constructor injection ensures immutability, makes dependencies explicit, improves testability (easy to create mocks without Spring context), and aligns with Spring Framework best practices.

### V. Smart/Presentational Component Separation (Frontend)

Angular components MUST follow Smart/Presentational (Container/Dumb) pattern for clear separation of concerns.

**Mandatory Requirements:**
- **Smart Components (Containers):**
  - Located in `features/{feature}/containers/`
  - Manage business logic and state
  - Interact with NgRx Store and services
  - Handle events from Presentational components
  - Use `inject()` function for dependency injection (Angular 14+)

- **Presentational Components (Dumb):**
  - Located in `features/{feature}/components/` or `shared/components/`
  - Receive data via `@Input()` (preferably signals)
  - Emit events via `@Output()`
  - NO knowledge of NgRx Store or services
  - MUST use OnPush change detection strategy
  - Pure, reusable, independently testable

**Rationale:** Separation improves testability (Presentational components can be tested in isolation), reusability (Dumb components work in any context), and maintainability (clear responsibilities).

### VI. Testing Strategy (NON-NEGOTIABLE)

Code MUST be thoroughly tested at multiple levels with minimum coverage thresholds enforced.

**Mandatory Requirements:**

**Backend:**
- Unit tests MUST cover ≥80% of service layer code (JUnit 5 + Mockito)
- Integration tests MUST cover all REST endpoints (Spring Boot Test + WebFlux Test)
- Integration tests MUST use Testcontainers for PostgreSQL (real database)
- WebSocket handlers MUST have integration tests with WebSocketClient
- Contract tests MUST validate OpenAPI specification compliance

**Frontend:**
- Unit tests MUST cover ≥70% of component/service code (Jasmine + Karma)
- NgRx store MUST have unit tests for reducers, effects, selectors
- Critical user flows MUST have E2E tests (Cypress):
  - US1: Create task → start timer → stop timer → complete task
  - US2: Create project → add member → assign task
  - US4: Generate report → export to Excel
- Integration tests MUST cover feature module interactions

**Quality Gates:**
- All tests MUST pass before merge to main branch
- Code coverage MUST NOT decrease below thresholds
- New features MUST include tests before implementation (Test-Driven Development encouraged but not mandatory)

**Rationale:** High test coverage prevents regressions, enables confident refactoring, and serves as living documentation. Integration tests catch issues that unit tests miss.

### VII. Performance & Scalability

The system MUST meet defined performance targets and scale gracefully under load.

**Mandatory Requirements:**
- API endpoints MUST respond within 200ms (p95 latency)
- Database queries MUST use indexes for frequently accessed fields (defined in Flyway migrations)
- Time-intensive operations (report generation, file uploads) MUST use async processing
- Frontend bundle size MUST be ≤2MB (gzipped) for initial load
- Lazy loading MUST be used for all feature modules
- Virtual scrolling (Angular CDK) MUST be used for lists >100 items
- OnPush change detection MUST be used for Presentational components
- Caching (Spring Cache + Caffeine) MUST be used for read-heavy data (user permissions, project members)

**Performance Targets (from Success Criteria):**
- SC-002: Timer updates with latency ≤1 second
- SC-004: Project report for 100 tasks in ≤5 seconds
- SC-005: Support 100 concurrent users
- SC-006: WebSocket notifications delivered in ≤2 seconds
- SC-008: Excel export for 500 records in ≤10 seconds
- SC-015: Drag & drop operations at ≥30 FPS

**Rationale:** Performance is a feature. Slow applications frustrate users and fail to deliver value. Performance must be designed in, not optimized later.

---

## Backend Best Practices

**Build Tool:**
- MUST use Gradle with Kotlin DSL (build.gradle.kts)

**Exception Handling:**
- MUST use centralized @ControllerAdvice with @ExceptionHandler methods
- MUST return consistent ErrorResponse DTO (timestamp, status, error, message, path, validationErrors)
- MUST handle reactive exceptions with onErrorResume/onErrorMap

**Logging:**
- MUST use SLF4J with Logback
- INFO level for business operations (task created, timer started, status changed, authentication)
- DEBUG level for development details (SQL queries, method traces, reactive operators)
- ERROR level for exceptions and critical errors
- WARN level for retry attempts and deprecated usage
- Production: INFO by default, DEBUG for specific packages during troubleshooting
- MUST use structured logging (JSON format) in production for ELK/Splunk parsing
- MUST include Correlation ID (MDC context) for request tracing

**Validation:**
- MUST use Bean Validation (JSR-380) on ALL DTO classes
- MUST use @Valid annotation on controller method parameters
- MUST create custom validators for business rules (@UniqueEmail, etc.)
- MUST use validation groups for different scenarios (Create vs Update)

**Code Style:**
- MUST follow Java conventions: PascalCase for classes, camelCase for methods/variables
- MUST use Lombok for boilerplate reduction (@RequiredArgsConstructor, @Data for DTOs)
- MUST use MapStruct for DTO ↔ Entity mapping where beneficial

---

## Frontend Best Practices

**State Management:**
- MUST use NgRx for global application state (auth, projects, tasks, timeEntries, notifications)
- MUST use Angular Signals for local component state (UI toggles, form state, loading indicators)
- MUST use toSignal() to integrate NgRx observables with Signals
- MUST use Facade services to hide NgRx complexity from components

**Dependency Injection:**
- SHOULD use inject() function for simple service injection (Angular 14+)
- MAY use constructor injection for services with @Inject tokens or complex initialization
- MUST NOT use field injection (@Autowired equivalent) - prohibited

**Change Detection:**
- MUST use OnPush strategy for ALL Presentational components
- Smart components MAY use Default strategy when necessary for WebSocket/async updates
- MUST ensure @Input values are immutable for OnPush components
- Signals automatically trigger change detection in OnPush components

**Forms:**
- MUST use Reactive Forms for ALL forms in the application
- Template-driven forms are PROHIBITED
- MUST use FormBuilder for form construction
- MUST implement custom validators for business rules
- MUST use async validators with debounce for backend validation
- MUST provide centralized error message mapping

**Code Style:**
- MUST use kebab-case for file names (task-list-container.component.ts)
- MUST use PascalCase for class names (TaskListContainerComponent)
- MUST use camelCase for variables/methods
- MUST use single quotes for strings
- MUST indent with 2 spaces

**Performance:**
- MUST use trackBy functions for all *ngFor directives
- MUST use async pipe for observables in templates
- MUST use deferrable views for non-critical content (Angular 17+)
- MUST optimize images with NgOptimizedImage directive

---

## Development Workflow

**Version Control:**
- Feature branches MUST follow naming convention: `feature/{feature-number}-{short-description}`
- Commits MUST have descriptive messages following Conventional Commits format
- Pull requests MUST reference related tasks/issues

**Code Review:**
- All code MUST be reviewed before merge to main
- Reviewers MUST verify:
  - Constitution principle compliance
  - Test coverage meets thresholds
  - No security vulnerabilities introduced
  - Performance impact considered
  - Documentation updated if needed

**Quality Gates:**
- All linter errors MUST be resolved (Checkstyle/SpotBugs for Java, ESLint for TypeScript)
- All unit and integration tests MUST pass
- Code coverage MUST NOT decrease below thresholds (80% backend, 70% frontend)
- Build MUST succeed without warnings

---

## Governance

**Constitution Authority:**
- This constitution supersedes all other development practices and guidelines
- When conflicts arise between constitution and other documentation, constitution prevails
- Constitution principles are NON-NEGOTIABLE unless explicitly amended

**Amendments:**
- Amendments require:
  1. Written proposal with rationale and impact analysis
  2. Team discussion and consensus
  3. Documentation update across all affected artifacts (spec, plan, tasks)
  4. Migration plan if existing code affected
  5. Version bump following semantic versioning

**Versioning:**
- MAJOR: Backward incompatible principle removals or redefinitions
- MINOR: New principles added or materially expanded guidance
- PATCH: Clarifications, wording fixes, non-semantic refinements

**Compliance Verification:**
- All PRs/code reviews MUST verify compliance with this constitution
- Violations MUST be rejected or justified with explicit rationale
- Complexity introduced MUST be justified against principles
- `/speckit.analyze` command MUST be run to detect constitution violations before implementation

**Enforcement:**
- Constitution violations discovered post-merge MUST be prioritized for remediation
- Repeated violations indicate need for principle clarification or team training
- Critical violations (Security-First, Reactive-First) block production deployment

**Documentation:**
- Constitution MUST be referenced in:
  - `plan.md` (Constitution Check section)
  - `README.md` (link to constitution)
  - New developer onboarding materials

---

**Version**: 1.0.0 | **Ratified**: 2025-11-14 | **Last Amended**: 2025-11-14
