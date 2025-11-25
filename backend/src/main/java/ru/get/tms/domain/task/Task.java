package ru.get.tms.domain.task;

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
 * Доменная сущность Task (Задача).
 *
 * <p>Представляет единицу работы в проекте. Задача может иметь исполнителя, статус, приоритет,
 * дедлайн, оценку времени. Поддерживается иерархия задач до 5 уровней (подзадачи).
 *
 * <p>Реализует модель данных из спецификации (см. data-model.md, таблица tasks).
 *
 * @see TaskStatus
 * @see TaskPriority
 */
@Table("tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

  /** Уникальный идентификатор задачи. */
  @Id private UUID id;

  /** Проект, к которому принадлежит задача (ссылка на projects.id). */
  @Column("project_id")
  private UUID projectId;

  /**
   * Родительская задача (для подзадач).
   *
   * <p>Null для задач верхнего уровня.
   */
  @Column("parent_task_id")
  private UUID parentTaskId;

  /** Название задачи (1-500 символов). */
  @Column("title")
  private String title;

  /** Описание задачи (rich text в виде HTML, опционально). */
  @Column("description")
  private String description;

  /** Исполнитель задачи (ссылка на users.id, опционально). */
  @Column("assignee_id")
  private UUID assigneeId;

  /** Создатель задачи (ссылка на users.id). */
  @Column("created_by")
  private UUID createdBy;

  /**
   * Статус задачи.
   *
   * @see TaskStatus
   */
  @Column("status")
  @Builder.Default
  private TaskStatus status = TaskStatus.NEW;

  /**
   * Приоритет задачи.
   *
   * @see TaskPriority
   */
  @Column("priority")
  @Builder.Default
  private TaskPriority priority = TaskPriority.MEDIUM;

  /** Дедлайн выполнения задачи (опционально). */
  @Column("deadline")
  private LocalDateTime deadline;

  /**
   * Оценка времени выполнения в часах (опционально).
   *
   * <p>Используется для планирования и сравнения с фактическим временем.
   */
  @Column("estimated_hours")
  private BigDecimal estimatedHours;

  /**
   * Уровень вложенности в иерархии (0-4).
   *
   * <p>0 - задача верхнего уровня, 4 - максимальная глубина (5 уровней).
   */
  @Column("hierarchy_level")
  @Builder.Default
  private Integer hierarchyLevel = 0;

  /**
   * Путь в иерархии для эффективных запросов поддерева.
   *
   * <p>Формат: /parent_id/current_id/ для быстрого поиска всех потомков.
   */
  @Column("hierarchy_path")
  private String hierarchyPath;

  /** Дата завершения задачи (заполняется при переходе в статус COMPLETED). */
  @Column("completed_at")
  private LocalDateTime completedAt;

  /** Дата создания задачи. */
  @Column("created_at")
  private LocalDateTime createdAt;

  /** Дата последнего обновления задачи. */
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
