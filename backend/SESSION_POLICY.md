# Session Inactivity Policy

**Требования**: FR-007.3, NFR-020, SC-001

## Обзор

Система реализует автоматическое истечение пользовательских сессий по неактивности для обеспечения безопасности данных.

## Механизм работы

### JWT-based Authentication

Система использует два типа токенов:

1. **Access Token** (Токен доступа)
   - **Время жизни**: 30 минут (по умолчанию)
   - **Назначение**: Используется для аутентификации API запросов
   - **Хранение**: В памяти приложения (не в localStorage)
   - **Обновление**: Автоматически обновляется через refresh token

2. **Refresh Token** (Токен обновления)
   - **Время жизни**: 7 дней
   - **Назначение**: Получение нового access token
   - **Хранение**: HttpOnly cookie (защита от XSS)
   - **Ротация**: Новый refresh token выдается при каждом обновлении

### Политика неактивности

#### Backend (Spring Security)

**Access Token истекает через 30 минут** после последней активности:

- Каждый API запрос с валидным access token считается активностью
- После истечения access token пользователь должен получить новый через refresh token
- Если refresh token также истек (7 дней без активности), требуется повторный вход

**Конфигурация**:

```yaml
jwt:
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:1800000}  # 30 минут (мс)
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800000}  # 7 дней (мс)
```

**Переменные окружения**:

```bash
# Настройка времени истечения access token (в миллисекундах)
JWT_ACCESS_EXPIRATION=1800000  # 30 минут (по умолчанию)

# Настройка времени истечения refresh token (в миллисекундах)
JWT_REFRESH_EXPIRATION=604800000  # 7 дней (по умолчанию)
```

#### Frontend (Angular - Idle Timer)

Клиентское приложение отслеживает активность пользователя:

- **Мониторинг событий**: клики мыши, нажатия клавиш, API запросы
- **Порог неактивности**: Синхронизирован с backend (30 минут)
- **Предупреждение**: За 2 минуты до истечения показывается диалог
- **Автоматический logout**: При достижении порога неактивности

См. `frontend/src/app/core/services/idle-timer.service.ts` для деталей реализации.

## Сценарии использования

### 1. Активный пользователь

```
User активен → Access token обновляется каждые 30 мин → Сессия продолжается
```

### 2. Неактивный пользователь (< 30 минут)

```
User неактивен 20 минут → 
Frontend показывает предупреждение (через 28 минут) →
User нажимает "Продолжить" → Access token обновляется → Сессия продолжается
```

### 3. Неактивный пользователь (> 30 минут)

```
User неактивен 30+ минут → 
Access token истекает →
Frontend автоматически делает logout →
Показывается уведомление: "Сессия завершена из-за неактивности" →
Redirect на /auth/login
```

### 4. Длительная неактивность (> 7 дней)

```
User не заходил 7+ дней →
Refresh token истекает →
Требуется повторная аутентификация (email + password)
```

## Безопасность

### Защита токенов

- **Access Token**: Хранится в памяти (не в localStorage/sessionStorage)
- **Refresh Token**: HttpOnly cookie с флагами:
  - `HttpOnly=true` (защита от XSS)
  - `Secure=true` (только HTTPS в production)
  - `SameSite=Strict` (защита от CSRF)

### Логирование

Все события сессий логируются:

```
INFO  - User session started: user_id=<uuid>, ip=<ip>
INFO  - Access token refreshed: user_id=<uuid>
WARN  - Session expired due to inactivity: user_id=<uuid>, last_activity=<timestamp>
```

### Audit Trail

События аутентификации записываются для аудита:

- Успешный вход
- Неудачная попытка входа
- Истечение сессии
- Принудительный logout

## Настройка

### Development

Для разработки можно увеличить время жизни токенов:

```bash
# .env.development
JWT_ACCESS_EXPIRATION=3600000  # 1 час
JWT_REFRESH_EXPIRATION=2592000000  # 30 дней
```

### Production

В production рекомендуется использовать значения по умолчанию:

```bash
# .env.production
JWT_ACCESS_EXPIRATION=1800000  # 30 минут
JWT_REFRESH_EXPIRATION=604800000  # 7 дней
```

### Testing

Для тестирования можно использовать короткие интервалы:

```bash
# application-test.yml
JWT_ACCESS_EXPIRATION=60000  # 1 минута
JWT_REFRESH_EXPIRATION=300000  # 5 минут
```

## Тестирование

См. integration тесты:

- `backend/src/test/java/ru/get/tms/integration/SessionInactivityTest.java`
- `frontend/tests/e2e/session-inactivity.spec.ts`

## Соответствие требованиям

| Требование | Описание | Статус |
|-----------|----------|--------|
| FR-007.3 | Автоматический logout после неактивности | ✅ Реализовано |
| NFR-020 | Настраиваемое время истечения сессии | ✅ Реализовано |
| SC-001 | Защита от несанкционированного доступа | ✅ Реализовано |

## Дополнительные ресурсы

- [Spring Security JWT Configuration](./src/main/java/ru/get/tms/security/JwtUtil.java)
- [Frontend Idle Timer Service](../frontend/src/app/core/services/idle-timer.service.ts)
- [OpenAPI Authentication Spec](../specs/001-team-task-manager/contracts/openapi.yaml)

