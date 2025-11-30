/**
 * Модели для работы с задачами.
 *
 * Соответствуют backend DTOs для обеспечения type-safe API взаимодействия.
 */

/** Статусы задачи (соответствует backend TaskStatus enum). */
export enum TaskStatus {
  NEW = 'NEW',
  IN_PROGRESS = 'IN_PROGRESS',
  TESTING = 'TESTING',
  BLOCKED = 'BLOCKED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

/** Приоритеты задачи (соответствует backend TaskPriority enum). */
export enum TaskPriority {
  URGENT = 'URGENT',
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
}

/** Основная модель задачи (соответствует backend TaskResponseDTO). */
export interface Task {
  id: string;
  projectId: string;
  parentTaskId?: string | null;
  title: string;
  description?: string | null;
  assigneeId?: string | null;
  createdBy: string;
  status: TaskStatus;
  priority: TaskPriority;
  deadline?: string | null; // ISO 8601 datetime string
  estimatedHours?: number | null;
  hierarchyLevel: number;
  hierarchyPath?: string | null;
  completedAt?: string | null; // ISO 8601 datetime string
  createdAt: string; // ISO 8601 datetime string
  updatedAt: string; // ISO 8601 datetime string

  // Enriched fields (опционально заполняются backend)
  assigneeName?: string;
  createdByName?: string;
  projectName?: string;
  projectColor?: string;
  subtasksCount?: number;
  completedSubtasksCount?: number;
  progressPercent?: number;
}

/** DTO для создания задачи (соответствует backend TaskCreateDTO). */
export interface TaskCreateRequest {
  title: string;
  description?: string;
  projectId: string;
  parentTaskId?: string;
  assigneeId?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  deadline?: string; // ISO 8601 datetime string
  estimatedHours?: number;
}

/** DTO для обновления статуса задачи. */
export interface TaskUpdateStatusRequest {
  status: TaskStatus;
}

/** Фильтры для списка задач. */
export interface TaskFilters {
  projectId?: string;
  assigneeId?: string;
  status?: TaskStatus;
  priority?: TaskPriority;
  searchQuery?: string;
}

/** UI-специфичные дополнительные свойства для отображения. */
export interface TaskUIState {
  isExpanded: boolean;
  isEditing: boolean;
  isDeleting: boolean;
}

