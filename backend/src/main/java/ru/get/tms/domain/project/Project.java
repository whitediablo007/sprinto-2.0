package ru.get.tms.domain.project;

import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Доменная сущность Project (Проект).
 *
 * <p>Представляет проект в системе - контейнер для задач и команд. Проект имеет владельца,
 * участников с разными ролями, настройки биллинга и временные рамки.
 *
 * <p>Реализует модель данных из спецификации (см. data-model.md, таблица projects).
 *
 * @see ProjectStatus
 * @see ProjectMemberRole
 */
@Table("projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

  /** Уникальный идентификатор проекта. */
  @Id private UUID id;

  /** Владелец проекта (ссылка на users.id). */
  @Column("owner_id")
  private UUID ownerId;

  /** Название проекта (1-255 символов). */
  @Column("name")
  private String name;

  /** Описание проекта (rich text в виде HTML, опционально). */
  @Column("description")
  private String description;

  /** Цвет проекта в HEX формате (#RRGGBB). */
  @Column("color")
  private String color;

  /**
   * Статус проекта.
   *
   * @see ProjectStatus
   */
  @Column("status")
  @Builder.Default
  private ProjectStatus status = ProjectStatus.ACTIVE;

  /**
   * Ставка биллинга проекта (руб/час).
   *
   * <p>Используется для расчета стоимости работ. Имеет приоритет над ставкой пользователя.
   */
  @Column("hourly_rate")
  private BigDecimal hourlyRate;

  /** Дата начала проекта (опционально). */
  @Column("start_date")
  private LocalDate startDate;

  /** Дата окончания проекта (опционально). */
  @Column("end_date")
  private LocalDate endDate;

  /** Дата создания проекта. */
  @Column("created_at")
  private LocalDateTime createdAt;

  /** Дата последнего обновления проекта. */
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
