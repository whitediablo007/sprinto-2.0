-- V4: Time Tracking
-- Таблица time_entries (записи времени)
CREATE TABLE time_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    duration_seconds INTEGER CHECK (duration_seconds >= 0),
    hourly_rate DECIMAL(10,2) CHECK (hourly_rate >= 0),
    cost DECIMAL(10,2) CHECK (cost >= 0),
    entry_type time_entry_type NOT NULL,
    description TEXT,
    deleted_at TIMESTAMP,
    deleted_by UUID,
    delete_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_time_entries_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE RESTRICT,
    CONSTRAINT fk_time_entries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_time_entries_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT time_entries_end_after_start CHECK (end_time IS NULL OR end_time > start_time)
);

-- Индексы для time_entries
CREATE INDEX idx_time_entries_task_id ON time_entries(task_id);
CREATE INDEX idx_time_entries_user_id ON time_entries(user_id);
CREATE INDEX idx_time_entries_start_time ON time_entries(start_time);
CREATE INDEX idx_time_entries_user_time ON time_entries(user_id, start_time);
CREATE INDEX idx_time_entries_deleted_at ON time_entries(deleted_at) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_time_entries_active_timer ON time_entries(user_id, end_time) WHERE end_time IS NULL;

-- Таблица time_entry_audit (аудит изменений записей времени)
CREATE TABLE time_entry_audit (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    time_entry_id UUID NOT NULL,
    changed_by UUID NOT NULL,
    change_type audit_change_type NOT NULL,
    change_reason TEXT,
    old_values JSONB,
    new_values JSONB,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_time_entry_audit_entry FOREIGN KEY (time_entry_id) REFERENCES time_entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_time_entry_audit_changed_by FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE RESTRICT
);

-- Индексы для time_entry_audit
CREATE INDEX idx_time_entry_audit_entry_id ON time_entry_audit(time_entry_id);
CREATE INDEX idx_time_entry_audit_changed_by ON time_entry_audit(changed_by);
CREATE INDEX idx_time_entry_audit_changed_at ON time_entry_audit(changed_at);

-- Триггер для автоматического обновления updated_at в time_entries
CREATE TRIGGER update_time_entries_updated_at
    BEFORE UPDATE ON time_entries
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

