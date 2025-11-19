-- V6: Calendar Settings and Performance Indexes
-- Таблица calendar_settings (настройки Google Calendar)
CREATE TABLE calendar_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL UNIQUE,
    google_calendar_id VARCHAR(255) NOT NULL,
    access_token_encrypted TEXT NOT NULL,
    refresh_token_encrypted TEXT NOT NULL,
    token_expires_at TIMESTAMP NOT NULL,
    sync_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sync_filters JSONB,
    last_sync_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calendar_settings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Индексы для calendar_settings
CREATE UNIQUE INDEX idx_calendar_settings_user_id ON calendar_settings(user_id);

-- Триггер для автоматического обновления updated_at в calendar_settings
CREATE TRIGGER update_calendar_settings_updated_at
    BEFORE UPDATE ON calendar_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Дополнительные performance индексы
-- Индексы для оптимизации запросов по времени (уже созданы в предыдущих миграциях, но добавляем дополнительные)

-- Индекс для поиска активных задач пользователя
CREATE INDEX IF NOT EXISTS idx_tasks_assignee_status_created ON tasks(assignee_id, status, created_at DESC);

-- Индекс для поиска задач по дедлайну
CREATE INDEX IF NOT EXISTS idx_tasks_deadline_status ON tasks(deadline, status) WHERE deadline IS NOT NULL;

-- Индекс для поиска записей времени по периоду
CREATE INDEX IF NOT EXISTS idx_time_entries_user_start_end ON time_entries(user_id, start_time, end_time) WHERE deleted_at IS NULL;

-- Индекс для поиска непрочитанных уведомлений
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread_created ON notifications(user_id, created_at DESC) WHERE read_at IS NULL;

-- Представление active_timers для быстрого доступа к активным таймерам
CREATE OR REPLACE VIEW active_timers AS
SELECT 
    te.id,
    te.user_id,
    te.task_id,
    te.start_time,
    EXTRACT(EPOCH FROM (NOW() - te.start_time))::INTEGER as elapsed_seconds,
    t.title as task_title,
    p.name as project_name,
    p.color as project_color
FROM time_entries te
JOIN tasks t ON t.id = te.task_id
JOIN projects p ON p.id = t.project_id
WHERE te.end_time IS NULL AND te.deleted_at IS NULL;

-- Представление task_progress для расчета прогресса задач с подзадачами
CREATE OR REPLACE VIEW task_progress AS
SELECT 
    t.id as task_id,
    COUNT(DISTINCT st.id) as total_subtasks,
    COUNT(DISTINCT st.id) FILTER (WHERE st.status IN ('COMPLETED', 'CANCELLED')) as completed_subtasks,
    CASE 
        WHEN COUNT(DISTINCT st.id) = 0 THEN 0
        ELSE ROUND((COUNT(DISTINCT st.id) FILTER (WHERE st.status IN ('COMPLETED', 'CANCELLED'))::numeric / 
                    COUNT(DISTINCT st.id)::numeric) * 100, 2)
    END as progress_percent
FROM tasks t
LEFT JOIN tasks st ON st.parent_task_id = t.id
GROUP BY t.id;

