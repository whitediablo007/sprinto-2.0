package ru.get.tms.dto.task;

import jakarta.validation.constraints.*;
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
 * DTO для создания новой задачи.
 *
 * <p>Используется в endpoint POST /api/tasks для создания задачи в проекте.
 *
 * <p>Валидация:
 *
 * <ul>
 *   <li>title: обязательное, 1-500 символов
 *   <li>description: опциональное, макс 10000 символов
 *   <li>projectId: обязательное
 *   <li>status: по умолчанию NEW
 *   <li>priority: по умолчанию MEDIUM
 *   <li>estimatedHours: опциональное, >= 0
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCreateDTO {

  @NotBlank(message = "Title is required")
  @Size(min = 1, max = 500, message = "Title must be between 1 and 500 characters")
  private String title;

  @Size(max = 10000, message = "Description must not exceed 10000 characters")
  private String description;

  @NotNull(message = "Project ID is required")
  private UUID projectId;

  private UUID parentTaskId;

  private UUID assigneeId;

  @Builder.Default private TaskStatus status = TaskStatus.NEW;

  @Builder.Default private TaskPriority priority = TaskPriority.MEDIUM;

  private LocalDateTime deadline;

  @DecimalMin(value = "0.0", inclusive = true, message = "Estimated hours must be non-negative")
  @DecimalMax(
      value = "999.99",
      inclusive = true,
      message = "Estimated hours must not exceed 999.99")
  private BigDecimal estimatedHours;
}
