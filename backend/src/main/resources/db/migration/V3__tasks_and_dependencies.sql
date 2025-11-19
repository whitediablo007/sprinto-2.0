-- V3: Tasks and Dependencies
-- Таблица tasks (задачи)
CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    project_id UUID NOT NULL,
    parent_task_id UUID,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    assignee_id UUID,
    created_by UUID NOT NULL,
    status task_status NOT NULL DEFAULT 'NEW',
    priority task_priority NOT NULL DEFAULT 'MEDIUM',
    deadline TIMESTAMP,
    estimated_hours DECIMAL(8,2) CHECK (estimated_hours >= 0),
    hierarchy_level INTEGER NOT NULL DEFAULT 0 CHECK (hierarchy_level >= 0 AND hierarchy_level <= 4),
    hierarchy_path VARCHAR(255),
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT tasks_title_length CHECK (LENGTH(title) >= 1 AND LENGTH(title) <= 500),
    CONSTRAINT tasks_no_self_parent CHECK (parent_task_id IS NULL OR parent_task_id != id)
);

-- Индексы для tasks
CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX idx_tasks_parent_task_id ON tasks(parent_task_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_deadline ON tasks(deadline);
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_tasks_assignee_status ON tasks(assignee_id, status);
CREATE INDEX idx_tasks_hierarchy_path ON tasks USING GIN (hierarchy_path);

-- Таблица task_dependencies (зависимости задач)
CREATE TABLE task_dependencies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    dependent_task_id UUID NOT NULL,
    blocking_task_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_dependencies_dependent FOREIGN KEY (dependent_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_dependencies_blocking FOREIGN KEY (blocking_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT task_dependencies_unique UNIQUE (dependent_task_id, blocking_task_id),
    CONSTRAINT task_dependencies_no_self CHECK (dependent_task_id != blocking_task_id)
);

-- Индексы для task_dependencies
CREATE INDEX idx_task_dependencies_dependent ON task_dependencies(dependent_task_id);
CREATE INDEX idx_task_dependencies_blocking ON task_dependencies(blocking_task_id);

-- Таблица tags (метки)
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    color VARCHAR(7) NOT NULL,
    project_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tags_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT tags_name_length CHECK (LENGTH(name) >= 1 AND LENGTH(name) <= 100),
    CONSTRAINT tags_color_format CHECK (color ~* '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT tags_name_project_unique UNIQUE (name, project_id)
);

-- Индексы для tags
CREATE INDEX idx_tags_project_id ON tags(project_id);

-- Таблица task_tags (связь задач и меток)
CREATE TABLE task_tags (
    task_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_tags_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    CONSTRAINT task_tags_pk PRIMARY KEY (task_id, tag_id)
);

-- Индексы для task_tags
CREATE INDEX idx_task_tags_task_id ON task_tags(task_id);
CREATE INDEX idx_task_tags_tag_id ON task_tags(tag_id);

-- Триггер для автоматического обновления updated_at в tasks
CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

