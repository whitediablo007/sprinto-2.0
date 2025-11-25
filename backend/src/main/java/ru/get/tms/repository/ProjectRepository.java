package ru.get.tms.repository;

import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.get.tms.domain.project.Project;
import ru.get.tms.domain.project.ProjectStatus;

/**
 * R2DBC репозиторий для работы с сущностью {@link Project}.
 *
 * <p>Предоставляет реактивные методы для CRUD операций и специализированных запросов к таблице
 * projects.
 */
public interface ProjectRepository extends ReactiveCrudRepository<Project, UUID> {

  /**
   * Находит все проекты, принадлежащие указанному владельцу.
   *
   * @param ownerId идентификатор владельца проекта
   * @return Flux проектов владельца
   */
  @Query("SELECT * FROM projects WHERE owner_id = :ownerId ORDER BY created_at DESC")
  Flux<Project> findByOwnerId(UUID ownerId);

  /**
   * Находит все проекты с указанным статусом.
   *
   * @param status статус проектов
   * @return Flux проектов с заданным статусом
   */
  @Query("SELECT * FROM projects WHERE status = :status ORDER BY created_at DESC")
  Flux<Project> findByStatus(ProjectStatus status);

  /**
   * Находит все проекты владельца с указанным статусом.
   *
   * @param ownerId идентификатор владельца проекта
   * @param status статус проектов
   * @return Flux проектов владельца с заданным статусом
   */
  @Query(
      "SELECT * FROM projects WHERE owner_id = :ownerId AND status = :status ORDER BY created_at"
          + " DESC")
  Flux<Project> findByOwnerIdAndStatus(UUID ownerId, ProjectStatus status);

  /**
   * Проверяет существование проекта с указанным именем у владельца.
   *
   * @param name название проекта
   * @param ownerId идентификатор владельца проекта
   * @return Mono с результатом проверки
   */
  @Query("SELECT EXISTS(SELECT 1 FROM projects WHERE name = :name AND owner_id = :ownerId)")
  Mono<Boolean> existsByNameAndOwnerId(String name, UUID ownerId);

  /**
   * Подсчитывает количество проектов владельца.
   *
   * @param ownerId идентификатор владельца проекта
   * @return Mono с количеством проектов
   */
  @Query("SELECT COUNT(*) FROM projects WHERE owner_id = :ownerId")
  Mono<Long> countByOwnerId(UUID ownerId);

  /**
   * Подсчитывает количество активных проектов владельца.
   *
   * @param ownerId идентификатор владельца проекта
   * @return Mono с количеством активных проектов
   */
  @Query("SELECT COUNT(*) FROM projects WHERE owner_id = :ownerId AND status = 'ACTIVE'")
  Mono<Long> countActiveByOwnerId(UUID ownerId);
}
