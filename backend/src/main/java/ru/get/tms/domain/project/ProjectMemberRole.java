package ru.get.tms.domain.project;

/**
 * Роли участников проекта.
 *
 * <p>Енум определяет уровни доступа участников к проекту:
 *
 * <ul>
 *   <li>PROJECT_OWNER - владелец проекта (полный доступ)
 *   <li>PROJECT_MEMBER - участник проекта (CRUD своих задач)
 *   <li>PROJECT_OBSERVER - наблюдатель (только чтение)
 * </ul>
 */
public enum ProjectMemberRole {
  /** Владелец проекта с полным доступом. */
  PROJECT_OWNER,

  /** Участник проекта, может создавать и редактировать свои задачи. */
  PROJECT_MEMBER,

  /** Наблюдатель с правами только на чтение. */
  PROJECT_OBSERVER
}
