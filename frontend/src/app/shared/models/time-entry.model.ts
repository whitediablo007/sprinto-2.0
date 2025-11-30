/**
 * Модели для работы с записями времени и таймерами.
 *
 * Соответствуют backend DTOs для обеспечения type-safe API взаимодействия.
 */

/** Типы записей времени (соответствует backend TimeEntryType enum). */
export enum TimeEntryType {
  TIMER = 'TIMER',
  MANUAL = 'MANUAL',
}

/** Основная модель записи времени (соответствует backend TimeEntryDTO). */
export interface TimeEntry {
  id: string;
  taskId: string;
  userId: string;
  startTime: string; // ISO 8601 datetime string
  endTime?: string | null; // ISO 8601 datetime string (null = активный таймер)
  durationSeconds?: number | null;
  hourlyRate?: number | null;
  cost?: number | null;
  entryType: TimeEntryType;
  description?: string | null;
  deletedAt?: string | null; // ISO 8601 datetime string
  deletedBy?: string | null;
  deleteReason?: string | null;
  createdAt: string; // ISO 8601 datetime string
  updatedAt: string; // ISO 8601 datetime string

  // Enriched fields (опционально заполняются backend)
  userName?: string;
  taskTitle?: string;
  projectName?: string;
  isActive?: boolean;
}

/** DTO для создания ручной записи времени. */
export interface TimeEntryCreateRequest {
  taskId: string;
  startTime: string; // ISO 8601 datetime string
  endTime: string; // ISO 8601 datetime string
  hourlyRate?: number;
  description?: string;
}

/** DTO для запуска таймера. */
export interface StartTimerRequest {
  taskId: string;
}

/** DTO для удаления записи времени. */
export interface DeleteTimeEntryRequest {
  reason?: string;
}

/** DTO для WebSocket обновлений таймера (соответствует backend TimerUpdateDTO). */
export interface TimerUpdate {
  timeEntryId: string;
  taskId: string;
  taskTitle: string;
  projectId: string;
  projectName: string;
  projectColor: string;
  startTime: string; // ISO 8601 datetime string
  elapsedSeconds: number;
  formattedCost: string;
  timestamp: string; // ISO 8601 datetime string
}

/** UI состояние для отображения таймера. */
export interface TimerUIState {
  isRunning: boolean;
  elapsedTime: string; // Форматированное время (HH:MM:SS)
  currentCost: string; // Форматированная стоимость
  isPaused: boolean;
}

