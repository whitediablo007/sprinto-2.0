package ru.get.tms.dto.timeentry;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для обновлений активного таймера через WebSocket.
 *
 * <p>Используется для отправки real-time обновлений состояния таймера на /user/queue/timer.
 *
 * <p>Содержит информацию о текущем активном таймере пользователя, включая elapsed time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerUpdateDTO {

  /** ID записи времени (таймера). */
  private UUID timeEntryId;

  /** ID задачи, на которой работает таймер. */
  private UUID taskId;

  /** Название задачи. */
  private String taskTitle;

  /** ID проекта. */
  private UUID projectId;

  /** Название проекта. */
  private String projectName;

  /** Цвет проекта (HEX). */
  private String projectColor;

  /** Время начала таймера. */
  private LocalDateTime startTime;

  /** Прошедшее время в секундах с момента start_time. */
  private Long elapsedSeconds;

  /** Текущая стоимость работы (рассчитывается динамически). */
  private String formattedCost;

  /** Timestamp обновления. */
  private LocalDateTime timestamp;
}
