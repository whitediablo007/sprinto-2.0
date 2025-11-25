package ru.get.tms.domain.task;

/**
 * Статусы задачи.
 *
 * <p>Енум определяет жизненный цикл задачи в системе:
 *
 * <ul>
 *   <li>NEW - новая задача
 *   <li>IN_PROGRESS - в работе
 *   <li>TESTING - на тестировании
 *   <li>BLOCKED - заблокирована
 *   <li>COMPLETED - завершена (финальный статус)
 *   <li>CANCELLED - отменена (финальный статус)
 * </ul>
 *
 * <p>Допустимые переходы состояний:
 *
 * <ul>
 *   <li>NEW → IN_PROGRESS, CANCELLED
 *   <li>IN_PROGRESS → TESTING, BLOCKED, COMPLETED, CANCELLED
 *   <li>TESTING → IN_PROGRESS, COMPLETED, CANCELLED
 *   <li>BLOCKED → IN_PROGRESS, CANCELLED
 *   <li>COMPLETED → (финальный)
 *   <li>CANCELLED → (финальный)
 * </ul>
 */
public enum TaskStatus {
  /** Новая задача, еще не взята в работу. */
  NEW,

  /** Задача в процессе выполнения. */
  IN_PROGRESS,

  /** Задача на тестировании. */
  TESTING,

  /** Задача заблокирована внешними факторами. */
  BLOCKED,

  /** Задача успешно завершена. */
  COMPLETED,

  /** Задача отменена. */
  CANCELLED
}
