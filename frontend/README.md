# Team Task Manager - Frontend

Frontend приложение для системы управления проектами и задачами для малых команд.

## Технологии

- **Angular 18+** - основной фреймворк
- **TypeScript 5.3+** - язык программирования
- **Tailwind CSS 3.4+** - утилитарный CSS фреймворк
- **PrimeNG 17+** - UI компоненты
- **NgRx 18+** - state management
- **RxJS 7.8+** - реактивное программирование
- **Chart.js** - графики и визуализация
- **FullCalendar** - календарь
- **Cypress** - E2E тестирование

## Требования

- Node.js 18+
- npm 9+ или yarn/pnpm

## Установка

```bash
npm install
```

## Запуск

```bash
npm start
```

Приложение будет доступно по адресу http://localhost:4200

## Сборка

```bash
npm run build
```

## Тестирование

```bash
# Unit тесты
npm test

# E2E тесты
npm run e2e
```

## Структура проекта

```
src/
├── app/
│   ├── core/              # Singleton сервисы, guards, interceptors
│   ├── shared/            # Shared компоненты, directives, pipes, models
│   ├── features/          # Feature modules
│   │   ├── auth/
│   │   ├── dashboard/
│   │   ├── projects/
│   │   ├── tasks/
│   │   ├── time-tracking/
│   │   └── ...
│   └── store/             # Root NgRx store
├── assets/                # Статические ресурсы
├── environments/          # Конфигурация окружений
└── styles/               # Глобальные стили
```

## Переменные окружения

- `API_URL` - URL backend API (по умолчанию: http://localhost:8080)
- `WS_URL` - URL WebSocket сервера (по умолчанию: ws://localhost:8080)

