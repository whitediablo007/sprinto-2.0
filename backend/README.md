# Team Task Manager - Backend

Backend приложение для системы управления проектами и задачами для малых команд.

## Технологии

- **Spring Boot 3.2.0** - основной фреймворк
- **Spring WebFlux** - реактивный веб-стек
- **Spring Security** - безопасность и аутентификация
- **Spring Data R2DBC** - реактивный доступ к данным
- **PostgreSQL** - база данных
- **Flyway** - миграции базы данных
- **Kotlin** - язык программирования
- **Gradle Kotlin DSL** - система сборки
- **JWT** - токены аутентификации
- **MapStruct** - маппинг DTO ↔ Entity
- **OpenAPI/Swagger** - документация API

## Требования

- Java 17+
- PostgreSQL 15+
- Gradle 8.5+

## Запуск

1. Убедитесь, что PostgreSQL запущен и доступен
2. Настройте переменные окружения или измените `application.yml`
3. Запустите приложение:

```bash
./gradlew bootRun
```

Или через IDE запустите `TeamTaskManagerApplication.kt`

## API Документация

После запуска приложения доступна Swagger UI:
- http://localhost:8080/swagger-ui.html
- http://localhost:8080/api-docs

## Структура проекта

```
src/
├── main/
│   ├── java/ru/get/tms/
│   │   ├── config/          # Конфигурация Spring
│   │   ├── domain/          # Доменные сущности
│   │   ├── repository/      # R2DBC репозитории
│   │   ├── service/         # Бизнес-логика
│   │   ├── api/             # REST контроллеры и WebSocket handlers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── mapper/          # MapStruct mappers
│   │   ├── security/       # Security: JWT, filters
│   │   ├── exception/       # Exception handling
│   │   └── util/            # Утилиты
│   └── resources/
│       ├── application.yml
│       └── db/migration/    # Flyway миграции
└── test/
    └── java/ru/get/tms/
        ├── integration/     # Интеграционные тесты
        ├── unit/           # Unit тесты
        └── contract/      # Contract тесты
```

## Переменные окружения

- `DB_USERNAME` - имя пользователя БД (по умолчанию: tms_user)
- `DB_PASSWORD` - пароль БД (по умолчанию: tms_password)
- `JWT_SECRET` - секретный ключ для JWT (обязательно в production)
- `SERVER_PORT` - порт приложения (по умолчанию: 8080)
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` - настройки SMTP

