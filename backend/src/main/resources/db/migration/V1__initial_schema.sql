-- V1: Initial Schema
-- Создание расширений PostgreSQL
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Создание енумов
CREATE TYPE project_status AS ENUM ('ACTIVE', 'ARCHIVED', 'COMPLETED');
CREATE TYPE task_status AS ENUM ('NEW', 'IN_PROGRESS', 'TESTING', 'BLOCKED', 'COMPLETED', 'CANCELLED');
CREATE TYPE task_priority AS ENUM ('URGENT', 'HIGH', 'MEDIUM', 'LOW');
CREATE TYPE project_member_role AS ENUM ('PROJECT_OWNER', 'PROJECT_MEMBER', 'PROJECT_OBSERVER');
CREATE TYPE time_entry_type AS ENUM ('TIMER', 'MANUAL');
CREATE TYPE notification_type AS ENUM ('TASK_ASSIGNED', 'STATUS_CHANGED', 'NEW_COMMENT', 'DEADLINE_APPROACHING', 'DEADLINE_EXPIRED', 'MENTIONED', 'ADDED_TO_PROJECT', 'PROJECT_UPDATED');
CREATE TYPE audit_change_type AS ENUM ('CREATED', 'UPDATED', 'DELETED', 'RESTORED');

-- Таблица users (пользователи)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512),
    hourly_rate DECIMAL(10,2) CHECK (hourly_rate >= 0),
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    notification_preferences JSONB NOT NULL DEFAULT '{}',
    do_not_disturb_until TIMESTAMP,
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Moscow',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_format CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_name_length CHECK (LENGTH(name) >= 2)
);

-- Индексы для users
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_admin ON users(is_admin);

-- Таблица password_reset_tokens (токены сброса пароля)
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Индексы для password_reset_tokens
CREATE UNIQUE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);

-- Триггер для автоматического обновления updated_at в users
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

