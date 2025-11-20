#!/bin/bash
# Скрипт для пересоздания базы данных в dev окружении

set -e

DB_NAME="${DB_NAME:-tms_db_dev}"
DB_USER="${DB_USER:-tms_user}"
DB_PASSWORD="${DB_PASSWORD:-tms_password}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"

echo "🗑️  Удаление базы данных $DB_NAME..."
PGPASSWORD="$DB_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"

echo "🏗️  Создание базы данных $DB_NAME..."
PGPASSWORD="$DB_PASSWORD" psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB_NAME;"

echo "✅ База данных $DB_NAME успешно пересоздана!"
echo "Теперь запустите приложение: ./gradlew bootRun"

