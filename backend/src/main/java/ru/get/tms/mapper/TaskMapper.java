package ru.get.tms.mapper;

import org.mapstruct.*;
import ru.get.tms.domain.task.Task;
import ru.get.tms.dto.task.TaskCreateDTO;
import ru.get.tms.dto.task.TaskResponseDTO;

/**
 * MapStruct mapper для преобразования между Task entity и DTO.
 *
 * <p>Использует Spring ComponentModel для интеграции с DI контейнером.
 *
 * <p>Стратегия unmapped properties: IGNORE для гибкости при добавлении новых полей.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

  /**
   * Преобразует TaskCreateDTO в Task entity.
   *
   * <p>Поля id, createdAt, updatedAt, completedAt игнорируются и устанавливаются сервисом.
   *
   * @param dto DTO с данными для создания задачи
   * @return Task entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "completedAt", ignore = true)
  @Mapping(target = "hierarchyPath", ignore = true)
  @Mapping(target = "hierarchyLevel", expression = "java(0)")
  Task toEntity(TaskCreateDTO dto);

  /**
   * Преобразует Task entity в TaskResponseDTO.
   *
   * <p>Дополнительные поля (assigneeName, projectName и т.д.) должны заполняться в сервисном слое
   * при необходимости.
   *
   * @param task Task entity
   * @return TaskResponseDTO
   */
  TaskResponseDTO toResponseDTO(Task task);

  /**
   * Частичное обновление Task entity из TaskCreateDTO.
   *
   * <p>Обновляет только не-null поля из DTO. Используется в операции PATCH/PUT.
   *
   * @param dto DTO с обновленными данными
   * @param task существующая Task entity для обновления
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "createdBy", ignore = true)
  @Mapping(target = "hierarchyPath", ignore = true)
  @Mapping(target = "hierarchyLevel", ignore = true)
  @Mapping(target = "completedAt", ignore = true)
  void updateEntityFromDto(TaskCreateDTO dto, @MappingTarget Task task);
}
