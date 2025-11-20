# Code Review Checklist

**Проект**: Team Task Manager  
**Требование**: NFR-027, Constitution Compliance

## Обзор

Данный чеклист используется при code review для обеспечения соответствия кода стандартам проекта и Constitution.

---

## Reactive Error Handling (NFR-027) ✅

**Критично**: Все WebFlux-сервисы должны использовать единый паттерн реактивной обработки ошибок.

### Обязательные проверки

- [ ] **Используется onErrorResume/onErrorMap**: Все `Mono`/`Flux` операции обрабатывают ошибки через реактивные операторы
- [ ] **Нет `.block()` в сервисах**: Отсутствуют блокирующие операции в реактивных pipeline
- [ ] **switchIfEmpty с handleNotFound**: При работе с `Mono.empty()` используется `ReactiveErrorHandler.handleNotFound()`
- [ ] **Доменные исключения**: Все бизнес-ошибки используют классы, расширяющие `DomainException`
- [ ] **Нет обрыва потоков**: Ошибки не приводят к завершению реактивного потока без обработки
- [ ] **Логирование**: Ошибки логируются с соответствующим уровнем (DEBUG/WARN/ERROR)
- [ ] **User-friendly сообщения**: Возвращаемые ошибки содержат понятные пользователю сообщения

### Примеры правильного использования

✅ **Хорошо**:
```java
public Mono<User> findUser(UUID id) {
  return userRepository.findById(id)
      .switchIfEmpty(ReactiveErrorHandler.handleNotFound("User", id))
      .onErrorResume(ReactiveErrorHandler::handleError);
}
```

❌ **Плохо**:
```java
public Mono<User> findUser(UUID id) {
  return userRepository.findById(id)
      .doOnError(e -> log.error("Error", e)); // Doesn't handle error!
}
```

---

## Constitution Compliance

### I. Reactive-First Architecture (NON-NEGOTIABLE)

- [ ] **Project Reactor**: Используются `Mono<T>` и `Flux<T>` для всех асинхронных операций
- [ ] **Неблокирующий I/O**: Все I/O операции выполняются через реактивные драйверы (R2DBC, WebClient)
- [ ] **Блокирующие операции изолированы**: Если блокировка неизбежна, используется `Schedulers.boundedElastic()`
- [ ] **WebSocket реактивный**: WebSocket handlers используют реактивные потоки с backpressure

### II. Modular Monolith Architecture

- [ ] **Четкие доменные границы**: Модули не имеют циклических зависимостей
- [ ] **Явные интерфейсы**: Кросс-модульная коммуникация через интерфейсы сервисов
- [ ] **Package структура**: Код организован по доменам, а не по техническим слоям

### III. Security-First

- [ ] **JWT валидация**: Все защищенные endpoints проверяют токены
- [ ] **Bean Validation**: DTO содержат JSR-380 аннотации
- [ ] **@PreAuthorize**: Авторизация реализована через аннотации
- [ ] **Пароли хэшированы**: Используется BCrypt с cost factor 12+

### IV. Constructor Injection (Backend)

- [ ] **@RequiredArgsConstructor**: Зависимости инжектятся через конструктор
- [ ] **private final fields**: Зависимости объявлены как final
- [ ] **Нет @Autowired**: Field injection не используется

### V. Smart/Presentational Component Separation (Frontend)

- [ ] **Smart компоненты в containers/**: Взаимодействие со store/services
- [ ] **Dumb компоненты в components/**: Только @Input/@Output
- [ ] **OnPush change detection**: Все presentational компоненты используют OnPush

### VI. Testing Strategy (NON-NEGOTIABLE)

- [ ] **Unit тесты**: Все сервисы покрыты unit тестами
- [ ] **Integration тесты**: Критичные flow протестированы end-to-end
- [ ] **StepVerifier**: Реактивные потоки тестируются через StepVerifier
- [ ] **Testcontainers**: Интеграционные тесты используют реальную БД

---

## Backend Specific

### Code Style

- [ ] **Google Java Format**: Код отформатирован через Spotless
- [ ] **Javadoc**: Публичные методы документированы
- [ ] **Логирование**: SLF4J с соответствующими уровнями
- [ ] **Нет магических чисел**: Константы вынесены в переменные

### Error Handling

- [ ] **DomainException иерархия**: Используются специфичные исключения
  - `ResourceNotFoundException` → 404
  - `UnauthorizedException` → 401
  - `ForbiddenException` → 403
  - `BusinessLogicException` → 422
  - `ConflictException` → 409
- [ ] **ReactiveErrorHandler**: Используется для маппинга ошибок
- [ ] **Контекст в логах**: Ошибки логируются с достаточным контекстом

### Database

- [ ] **R2DBC Queries**: Все запросы неблокирующие
- [ ] **Flyway миграции**: Изменения схемы через миграции
- [ ] **Индексы**: Частые запросы оптимизированы индексами
- [ ] **Транзакции**: `@Transactional` на методах, изменяющих данные

### REST API

- [ ] **OpenAPI документация**: Endpoints документированы
- [ ] **HTTP статусы**: Корректные коды ответов (200, 201, 404, 409, etc.)
- [ ] **Валидация**: DTO валидируются через Bean Validation
- [ ] **ErrorResponse**: Ошибки возвращают стандартный ErrorResponse

---

## Frontend Specific

### Angular

- [ ] **Standalone components**: Новые компоненты используют standalone API
- [ ] **inject() function**: DI через inject() в новом коде
- [ ] **Reactive Forms**: Template-driven forms не используются
- [ ] **trackBy**: Все `*ngFor` имеют trackBy функцию
- [ ] **async pipe**: Observable подписки через async pipe

### NgRx

- [ ] **Feature stores**: State изолирован по feature modules
- [ ] **Facade pattern**: Компоненты взаимодействуют через facade сервисы
- [ ] **Actions типизированы**: Используется createAction
- [ ] **Effects тестируются**: Все effects покрыты тестами

### UI/UX

- [ ] **Tailwind CSS**: Используются utility классы
- [ ] **PrimeNG components**: Переиспользуются готовые компоненты
- [ ] **Loading states**: Отображается состояние загрузки
- [ ] **Error handling**: Ошибки показываются пользователю

---

## Performance

- [ ] **Индексы БД**: Frequent queries оптимизированы
- [ ] **Virtual scrolling**: Списки >100 items используют virtual scroll
- [ ] **Lazy loading**: Feature modules загружаются лениво
- [ ] **Bundle size**: Frontend bundle ≤2MB gzipped
- [ ] **API latency**: p95 ≤200ms

---

## Documentation

- [ ] **README обновлен**: Новые фичи документированы
- [ ] **API documentation**: OpenAPI spec актуален
- [ ] **Architecture docs**: Значимые решения описаны
- [ ] **Comments**: Сложная логика прокомментирована

---

## Примечания для Reviewer

### Критические нарушения (блокируют merge)

1. ❌ Блокирующие операции (`.block()`) в реактивных сервисах
2. ❌ Отсутствие обработки ошибок в reactive pipeline
3. ❌ Пароли в plain text или слабое хэширование
4. ❌ SQL injection уязвимости
5. ❌ Падающие тесты

### Рекомендации (исправить желательно)

1. ⚠️ Недостаточное логирование
2. ⚠️ Отсутствие Javadoc на публичных методах
3. ⚠️ Дублирование кода
4. ⚠️ Неоптимальные запросы к БД
5. ⚠️ Магические числа без констант

---

## Дополнительные ресурсы

- [REACTIVE_ERROR_HANDLING.md](./REACTIVE_ERROR_HANDLING.md) - Паттерны обработки ошибок
- [Constitution](../.specify/memory/constitution.md) - Архитектурные принципы
- [CODE_FORMATTING.md](./CODE_FORMATTING.md) - Стандарты форматирования
- [SESSION_POLICY.md](./SESSION_POLICY.md) - Политика сессий

