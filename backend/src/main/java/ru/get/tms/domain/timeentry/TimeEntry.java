package ru.get.tms.domain.timeentry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Доменная сущность TimeEntry (Запись времени).
 *
 * <p>Представляет учет рабочего времени на задаче. Записи могут создаваться автоматически через
 * таймер или вручную пользователем. Поддерживается soft delete для сохранения истории.
 *
 * <p>Биллинг рассчитывается на основе длительности и ставки (приоритет: проект > пользователь).
 *
 * <p>Реализует модель данных из спецификации (см. data-model.md, таблица time_entries).
 *
 * @see TimeEntryType
 */
@Table("time_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntry {

  /** Уникальный идентификатор записи времени. */
  @Id private UUID id;

  /** Задача, на которую затрачено время (ссылка на tasks.id). */
  @Column("task_id")
  private UUID taskId;

  /** Пользователь, который работал над задачей (ссылка на users.id). */
  @Column("user_id")
  private UUID userId;

  /** Время начала работы. */
  @Column("start_time")
  private LocalDateTime startTime;

  /**
   * Время окончания работы.
   *
   * <p>Null означает активный таймер (запись в процессе).
   */
  @Column("end_time")
  private LocalDateTime endTime;

  /**
   * Длительность в секундах.
   *
   * <p>Рассчитывается автоматически при остановке таймера: duration_seconds = end_time -
   * start_time.
   */
  @Column("duration_seconds")
  private Integer durationSeconds;

  /**
   * Ставка биллинга для этой записи (руб/час).
   *
   * <p>Копируется из настроек проекта или пользователя при создании записи и не изменяется.
   */
  @Column("hourly_rate")
  private BigDecimal hourlyRate;

  /**
   * Рассчитанная стоимость работы (руб).
   *
   * <p>Формула: cost = (duration_seconds / 3600) * hourly_rate
   */
  @Column("cost")
  private BigDecimal cost;

  /**
   * Тип записи времени.
   *
   * @see TimeEntryType
   */
  @Column("entry_type")
  @Builder.Default
  private TimeEntryType entryType = TimeEntryType.TIMER;

  /** Комментарий к записи времени (опционально). */
  @Column("description")
  private String description;

  /**
   * Timestamp soft delete.
   *
   * <p>Null - активная запись, не-null - удаленная запись (для аудита).
   */
  @Column("deleted_at")
  private LocalDateTime deletedAt;

  /** Пользователь, который удалил запись (ссылка на users.id, опционально). */
  @Column("deleted_by")
  private UUID deletedBy;

  /** Причина удаления записи (для аудита, опционально). */
  @Column("delete_reason")
  private String deleteReason;

  /** Дата создания записи. */
  @Column("created_at")
  private LocalDateTime createdAt;

  /** Дата последнего обновления записи. */
  @Column("updated_at")
  private LocalDateTime updatedAt;

  /**
   * Проверяет, является ли запись активным таймером.
   *
   * @return true если таймер активен (end_time == null), иначе false
   */
  public boolean isActiveTimer() {
    return endTime == null;
  }

  /**
   * Проверяет, удалена ли запись (soft delete).
   *
   * @return true если запись удалена (deleted_at != null), иначе false
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }
}
