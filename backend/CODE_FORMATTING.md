# Форматирование кода Backend

## Инструменты

В проекте используется **Spotless** для автоматического форматирования Java кода по стандарту Google Java Style.

## Команды

### Проверка форматирования

```bash
./gradlew spotlessCheck
```

Проверяет соответствие кода стандартам форматирования. Не изменяет файлы.

### Автоматическое форматирование

```bash
./gradlew spotlessApply
```

Автоматически форматирует весь код в соответствии со стандартами.

## Pre-commit Hook

При каждом коммите автоматически запускается проверка форматирования.

Если проверка не прошла:
1. Запустите `./gradlew spotlessApply` в директории `backend`
2. Добавьте изменения: `git add .`
3. Повторите коммит

## Правила форматирования

- **Стиль**: Google Java Format
- **Отступы**: 4 пробела
- **Импорты**: Автоматическое удаление неиспользуемых
- **Пробелы**: Автоматическая очистка trailing whitespace
- **Конец файла**: Всегда пустая строка в конце

## IDE Integration

### IntelliJ IDEA

1. Установите плагин "google-java-format"
2. Settings → Editor → Code Style → Java → Scheme → Import Scheme → IntelliJ IDEA code style XML
3. Выберите файл: `https://raw.githubusercontent.com/google/styleguide/gh-pages/intellij-java-google-style.xml`

### VS Code

1. Установите расширение "Language Support for Java"
2. В settings.json добавьте:
```json
{
  "java.format.settings.url": "https://raw.githubusercontent.com/google/styleguide/gh-pages/eclipse-java-google-style.xml"
}
```

## Отключение форматирования для блока кода

Если нужно отключить форматирование для конкретного блока:

```java
// @formatter:off
String unformatted = "This    will   not    be    formatted";
// @formatter:on
```

