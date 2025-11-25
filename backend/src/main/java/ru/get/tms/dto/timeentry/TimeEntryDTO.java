package ru.get.tms.dto.timeentry;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.get.tms.domain.timeentry.TimeEntryType;

/**
 * DTO для записи времени.
 *
 * <p>Используется для создания/редактирования записей времени и ответов от API.
 *
 * <p>Валидация для ручного создания записи:
 *
 * <ul>
 *   <li>taskId: обязательное
 *   <li>startTime: обязательное
 *   <li>endTime: обязательное для MANUAL записей
 *   <li>hourlyRate: опциональное, >= 0
 *   <li>description: опциональное, макс 1000 символов
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDTO {

  private UUID id;

  @NotNull(message = "Task ID is required")
  private UUID taskId;

  private UUID userId;

  @NotNull(message = "Start time is required")
  private LocalDateTime startTime;

  private LocalDateTime endTime;

  private Integer durationSeconds;

  @DecimalMin(value = "0.0", inclusive = true, message = "Hourly rate must be non-negative")
  private BigDecimal hourlyRate;

  private BigDecimal cost;

  @Builder.Default private TimeEntryType entryType = TimeEntryType.MANUAL;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;

  private LocalDateTime deletedAt;

  private UUID deletedBy;

  private String deleteReason;

  private LocalDateTime createdAt;

  private LocalDateTime updatedAt;

  /** Имя пользователя (для отображения в UI). */
  private String userName;

  /** Название задачи (для отображения в UI). */
  private String taskTitle;

  /** Название проекта (для отображения в UI). */
  private String projectName;

  /** Признак активного таймера. */
  private Boolean isActive;
}
