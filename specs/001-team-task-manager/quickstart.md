# Быстрый старт разработки

**Проект**: Система управления проектами и задачами для малых команд  
**Дата**: 2025-11-14  
**Версия**: 1.0

## Обзор

Это руководство поможет настроить локальное окружение для разработки и запустить проект за 15-20 минут.

---

## Предварительные требования

### Обязательно

- **Java Development Kit (JDK)**: 17 или выше (LTS)
  ```bash
  java -version  # должна быть 17+
  ```

- **Node.js**: 18.x или выше
  ```bash
  node -version  # должна быть 18+
  npm -version
  ```

- **PostgreSQL**: 15 или выше
  ```bash
  psql --version  # должна быть 15+
  ```

- **Git**: Для клонирования репозитория
  ```bash
  git --version
  ```

### Рекомендуется

- **Docker & Docker Compose**: Для запуска PostgreSQL в контейнере (альтернатива локальной установке)
- **IntelliJ IDEA** или **VS Code**: IDE для разработки
- **Postman** или **Insomnia**: Для тестирования API
- **Angular CLI**: Для команд Angular
  ```bash
  npm install -g @angular/cli
  ```

---

## Шаг 1: Клонирование репозитория

```bash
git clone https://github.com/your-org/team-task-manager.git
cd team-task-manager
```

**Структура проекта**:
```
team-task-manager/
├── backend/          # Spring Boot приложение
├── frontend/         # Angular приложение
├── docs/             # Документация
├── specs/            # Спецификации фич
└── README.md
```

---

## Шаг 2: Настройка Backend

### 2.1. Установка PostgreSQL

**Вариант A: Используя Docker (рекомендуется)**

Создайте `docker-compose.yml` в корне проекта:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15-alpine
    container_name: tms-postgres
    environment:
      POSTGRES_DB: team_task_manager
      POSTGRES_USER: tms_user
      POSTGRES_PASSWORD: tms_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Запустите контейнер:

```bash
docker-compose up -d
```

Проверьте, что PostgreSQL запущен:

```bash
docker ps  # должен быть контейнер tms-postgres
```

**Вариант B: Локальная установка PostgreSQL**

1. Установите PostgreSQL 15+
2. Создайте БД и пользователя:

```sql
CREATE DATABASE team_task_manager;
CREATE USER tms_user WITH PASSWORD 'tms_password';
GRANT ALL PRIVILEGES ON DATABASE team_task_manager TO tms_user;
```

### 2.2. Конфигурация Backend

Перейдите в директорию backend:

```bash
cd backend
```

**Создайте файл `src/main/resources/application-dev.yml`**:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/team_task_manager
    username: tms_user
    password: tms_password
  flyway:
    url: jdbc:postgresql://localhost:5432/team_task_manager
    user: tms_user
    password: tms_password
    enabled: true
    baseline-on-migrate: true
  
logging:
  level:
    com.sprinto.tms: DEBUG
    org.springframework.r2dbc: DEBUG

jwt:
  secret: your-secret-key-change-in-production-min-256-bits
  access-token-expiration: 900000  # 15 minutes in ms
  refresh-token-expiration: 604800000  # 7 days in ms

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

**Примечание**: В production используйте переменные окружения для чувствительных данных!

### 2.3. Установка зависимостей и сборка

**Если используется Gradle**:

```bash
./gradlew clean build
```

**Если используется Maven**:

```bash
./mvnw clean install
```

### 2.4. Применение миграций БД

Flyway автоматически применит миграции при первом запуске приложения.

Проверьте наличие файлов миграций:

```bash
ls src/main/resources/db/migration/
# Должны быть файлы V1__initial_schema.sql, V2__add_indexes.sql и т.д.
```

### 2.5. Запуск Backend

**Gradle**:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**Maven**:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

**Или через IDE**:
- IntelliJ IDEA: Откройте `BackendApplication.java` → Run → Edit Configurations → Add VM options: `-Dspring.profiles.active=dev`

**Проверка**:

Backend должен запуститься на `http://localhost:8080`

Откройте в браузере:
- Health check: `http://localhost:8080/actuator/health` → должен вернуть `{"status":"UP"}`
- API docs: `http://localhost:8080/swagger-ui.html` → должен открыться Swagger UI

---

## Шаг 3: Настройка Frontend

### 3.1. Установка зависимостей

Перейдите в директорию frontend:

```bash
cd ../frontend
```

Установите npm пакеты:

```bash
npm install
```

**Если возникли ошибки**, попробуйте очистить кэш:

```bash
npm cache clean --force
rm -rf node_modules package-lock.json
npm install
```

### 3.2. Конфигурация Environment

**Файл `src/environments/environment.ts`** (для разработки):

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'http://localhost:8080/ws',
  googleOAuthClientId: 'your-google-client-id-here'  // опционально для dev
};
```

**Файл `src/environments/environment.prod.ts`** (для production):

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://api.sprinto.com/api',
  wsUrl: 'wss://api.sprinto.com/ws',
  googleOAuthClientId: 'your-production-google-client-id'
};
```

### 3.3. Запуск Frontend

```bash
ng serve
```

Или с указанием порта:

```bash
ng serve --port 4200
```

**Проверка**:

Frontend должен запуститься на `http://localhost:4200`

Откройте в браузере → должна появиться страница входа

---

## Шаг 4: Первый запуск и тестирование

### 4.1. Создание тестового пользователя

**Вариант A: Через Swagger UI**

1. Откройте `http://localhost:8080/swagger-ui.html`
2. Найдите `POST /api/auth/register`
3. Нажмите "Try it out"
4. Введите данные:

```json
{
  "email": "admin@example.com",
  "password": "password123",
  "name": "Admin User"
}
```

5. Нажмите "Execute" → получите access token

**Вариант B: Через curl**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "password": "password123",
    "name": "Admin User"
  }'
```

Ответ:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "uuid",
    "email": "admin@example.com",
    "name": "Admin User",
    ...
  }
}
```

### 4.2. Вход через Frontend

1. Откройте `http://localhost:4200`
2. Введите email: `admin@example.com`, пароль: `password123`
3. Нажмите "Войти" → вы должны попасть на главную страницу (личный дашборд)

### 4.3. Создание тестового проекта

После входа:

1. Нажмите кнопку "Создать проект"
2. Заполните форму:
   - Название: "Test Project"
   - Описание: "My first project"
   - Цвет: выберите любой
3. Сохраните → проект появится в списке

### 4.4. Создание задачи

1. Откройте созданный проект
2. Нажмите "Создать задачу"
3. Заполните:
   - Название: "Setup development environment"
   - Статус: NEW
   - Приоритет: HIGH
4. Сохраните → задача появится в списке

### 4.5. Запуск таймера

1. Откройте задачу
2. Нажмите кнопку "Запустить таймер"
3. Таймер должен начать отсчет в реальном времени (обновление каждую секунду через WebSocket)
4. Нажмите "Остановить" → время сохранится

---

## Шаг 5: Проверка интеграции

### 5.1. WebSocket соединение

Откройте Developer Tools в браузере → вкладка "Network" → фильтр "WS"

Вы должны увидеть WebSocket соединение к `ws://localhost:8080/ws`

**Статус**: 101 Switching Protocols (успешное подключение)

### 5.2. Real-time обновления

**Тест 1: Таймер**
- Запустите таймер на задаче
- Таймер должен обновляться каждую секунду без перезагрузки страницы

**Тест 2: Уведомления**
- Откройте приложение в двух браузерах (или вкладках) с разными пользователями
- Назначьте задачу одному пользователю от имени другого
- Уведомление должно появиться мгновенно (через WebSocket)

### 5.3. API тесты

**Через Postman**:

Импортируйте OpenAPI спецификацию:
- File → Import → Upload `specs/001-team-task-manager/contracts/openapi.yaml`
- Коллекция с endpoints появится в Postman

**Примеры запросов**:

1. **Получить профиль пользователя**:
```
GET http://localhost:8080/api/users/me
Headers:
  Authorization: Bearer {access_token}
```

2. **Получить список проектов**:
```
GET http://localhost:8080/api/projects
Headers:
  Authorization: Bearer {access_token}
```

3. **Создать задачу**:
```
POST http://localhost:8080/api/tasks
Headers:
  Authorization: Bearer {access_token}
  Content-Type: application/json
Body:
{
  "projectId": "uuid",
  "title": "New task",
  "status": "NEW",
  "priority": "MEDIUM"
}
```

---

## Шаг 6: Запуск тестов

### Backend Tests

```bash
cd backend

# Unit тесты
./gradlew test

# Интеграционные тесты (требуется PostgreSQL)
./gradlew integrationTest

# Все тесты
./gradlew check
```

**Просмотр отчета**:
- Gradle: `build/reports/tests/test/index.html`
- Maven: `target/surefire-reports/index.html`

### Frontend Tests

```bash
cd frontend

# Unit тесты (Jasmine + Karma)
npm run test

# E2E тесты (Cypress)
npm run e2e

# Lint проверка
npm run lint
```

---

## Дополнительные инструменты разработки

### Backend: Spring Boot DevTools

Для hot reload при изменении кода добавьте в `build.gradle`:

```gradle
dependencies {
    developmentOnly 'org.springframework.boot:spring-boot-devtools'
}
```

Или в `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### Frontend: Angular Hot Reload

Angular CLI уже поддерживает hot reload. При изменении файлов приложение автоматически перезагрузится.

### Database Management

**pgAdmin** (GUI для PostgreSQL):

```bash
docker run -d \
  --name pgadmin \
  -p 5050:80 \
  -e PGADMIN_DEFAULT_EMAIL=admin@example.com \
  -e PGADMIN_DEFAULT_PASSWORD=admin \
  dpage/pgadmin4
```

Откройте `http://localhost:5050`, добавьте сервер PostgreSQL (`localhost:5432`).

**DBeaver** (альтернатива): Скачайте с https://dbeaver.io/

---

## Отладка

### Backend Debugging (IntelliJ IDEA)

1. Откройте `BackendApplication.java`
2. Установите breakpoint на нужной строке (кликните слева от номера строки)
3. Run → Debug 'BackendApplication' (Shift + F9)
4. Выполните запрос к API → выполнение остановится на breakpoint

### Frontend Debugging (Chrome DevTools)

1. Откройте Chrome DevTools (F12)
2. Вкладка "Sources" → найдите `.ts` файл (webpack загружает source maps)
3. Установите breakpoint
4. Выполните действие в UI → выполнение остановится на breakpoint

### WebSocket Debugging

**Chrome DevTools**:
- Вкладка "Network" → фильтр "WS"
- Кликните на WebSocket соединение
- Вкладка "Messages" → просмотр всех сообщений

**Backend логи**:

Включите DEBUG логирование для WebSocket в `application-dev.yml`:

```yaml
logging:
  level:
    org.springframework.web.socket: DEBUG
    org.springframework.messaging: DEBUG
```

---

## Общие проблемы и решения

### Проблема 1: Backend не запускается

**Ошибка**: `Failed to configure a DataSource`

**Решение**:
- Проверьте, что PostgreSQL запущен: `docker ps` или `pg_isready`
- Проверьте credentials в `application-dev.yml`
- Убедитесь, что БД `team_task_manager` создана

### Проблема 2: Frontend не подключается к Backend

**Ошибка**: `CORS error` или `Connection refused`

**Решение**:
- Убедитесь, что backend запущен на `http://localhost:8080`
- Проверьте `apiUrl` в `environment.ts`
- Backend должен разрешать CORS для `http://localhost:4200` (конфигурация в `WebConfig.java`)

### Проблема 3: WebSocket не подключается

**Ошибка**: `WebSocket connection failed`

**Решение**:
- Проверьте, что JWT токен валиден (не истек)
- Проверьте `wsUrl` в `environment.ts`
- Проверьте browser console для деталей ошибки
- Убедитесь, что firewall не блокирует WebSocket

### Проблема 4: Flyway миграции не применяются

**Ошибка**: `Flyway validation failed`

**Решение**:
- Очистите БД: `DROP DATABASE team_task_manager; CREATE DATABASE team_task_manager;`
- Удалите таблицу Flyway: `DELETE FROM flyway_schema_history;`
- Перезапустите backend

### Проблема 5: npm install завершается с ошибками

**Решение**:
```bash
rm -rf node_modules package-lock.json
npm cache clean --force
npm install --legacy-peer-deps
```

---

## Переменные окружения

Для production развертывания используйте переменные окружения вместо hardcoded значений.

**Backend (`application-prod.yml`)**:

```yaml
spring:
  r2dbc:
    url: ${DATABASE_URL}
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}

jwt:
  secret: ${JWT_SECRET}
  
google:
  oauth:
    client-id: ${GOOGLE_OAUTH_CLIENT_ID}
    client-secret: ${GOOGLE_OAUTH_CLIENT_SECRET}
```

**Frontend (build time)**:

```bash
ng build --configuration production
```

Environment переменные подставятся из `environment.prod.ts`.

---

## Следующие шаги

После успешного запуска:

1. **Изучите документацию**:
   - [API Reference](./contracts/openapi.yaml) - OpenAPI спецификация
   - [WebSocket Protocol](./contracts/websocket.md) - протокол real-time коммуникации
   - [Data Model](./data-model.md) - модель данных и схема БД

2. **Настройте интеграции** (опционально для dev):
   - Google Calendar OAuth (требуется Google Cloud Console setup)
   - Email SMTP для уведомлений

3. **Начните разработку**:
   - Выберите задачу из `/speckit.tasks`
   - Создайте feature branch
   - Реализуйте функционал
   - Напишите тесты
   - Создайте Pull Request

---

## Полезные команды

### Backend

```bash
# Запуск
./gradlew bootRun --args='--spring.profiles.active=dev'

# Тесты
./gradlew test
./gradlew integrationTest

# Сборка JAR
./gradlew bootJar

# Проверка code style
./gradlew checkstyleMain
```

### Frontend

```bash
# Запуск dev server
ng serve

# Сборка production
ng build --configuration production

# Тесты
npm run test          # Unit тесты
npm run test:coverage # С coverage отчетом
npm run e2e           # E2E тесты

# Lint
npm run lint
npm run lint:fix      # С автоисправлением
```

### Docker

```bash
# Запуск PostgreSQL
docker-compose up -d

# Остановка
docker-compose down

# Просмотр логов
docker-compose logs -f postgres

# Вход в контейнер
docker exec -it tms-postgres psql -U tms_user -d team_task_manager
```

---

## Контакты и поддержка

- **Документация**: `docs/` директория в репозитории
- **Issues**: GitHub Issues для bug reports и feature requests
- **Slack**: #team-task-manager-dev канал
- **Email**: dev-team@sprinto.com

---

**Статус**: ✅ Готово к разработке  
**Время на setup**: ~15-20 минут  
**Последнее обновление**: 2025-11-14

