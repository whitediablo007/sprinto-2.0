package ru.get.tms.dto.task;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.get.tms.domain.task.TaskPriority;
import ru.get.tms.domain.task.TaskStatus;

/**
 * DTO для ответа с данными задачи.
 *
 * <p>Используется в endpoints GET /api/tasks/{id}, POST /api/tasks и других операциях с задачами.
 *
 * <p>Содержит полную информацию о задаче включая метаданные создания/обновления.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponseDTO {

  private UUID id;

  private UUID projectId;

  private UUID parentTaskId;

  private String title;

  private String description;

  private UUID assigneeId;

  private UUID createdBy;

  private TaskStatus status;

  private TaskPriority priority;

  private LocalDateTime deadline;

  private BigDecimal estimatedHours;

  private Integer hierarchyLevel;

  private String hierarchyPath;

  private LocalDateTime completedAt;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  /** Имя исполнителя (опционально, для удобства отображения). */
  private String assigneeName;

  /** Имя создателя задачи. */
  private String createdByName;

  /** Название проекта. */
  private String projectName;

  /** Цвет проекта. */
  private String projectColor;

  /** Количество подзадач (опционально). */
  private Long subtasksCount;

  /** Количество завершенных подзадач (опционально). */
  private Long completedSubtasksCount;

  /** Прогресс выполнения подзадач в процентах (0-100, опционально). */
  private BigDecimal progressPercent;
}
