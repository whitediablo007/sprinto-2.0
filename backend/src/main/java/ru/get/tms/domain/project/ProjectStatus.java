package ru.get.tms.domain.project;

/**
 * Статусы проекта.
 *
 * <p>Енум определяет возможные состояния проекта в системе:
 *
 * <ul>
 *   <li>ACTIVE - активный проект, доступен для работы
 *   <li>ARCHIVED - архивный проект (read-only)
 *   <li>COMPLETED - завершенный проект
 * </ul>
 */
public enum ProjectStatus {
  /** Активный проект, доступен для работы. */
  ACTIVE,

  /** Архивный проект (read-only). */
  ARCHIVED,

  /** Завершенный проект. */
  COMPLETED
}
