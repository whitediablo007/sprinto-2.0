package ru.get.tms.mapper;

import org.mapstruct.*;
import ru.get.tms.domain.timeentry.TimeEntry;
import ru.get.tms.dto.timeentry.TimeEntryDTO;

/**
 * MapStruct mapper для преобразования между TimeEntry entity и DTO.
 *
 * <p>Использует Spring ComponentModel для интеграции с DI контейнером.
 *
 * <p>Стратегия unmapped properties: IGNORE для гибкости при добавлении новых полей.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimeEntryMapper {

  /**
   * Преобразует TimeEntryDTO в TimeEntry entity.
   *
   * <p>Поля id, userId, durationSeconds, cost, createdAt, updatedAt игнорируются и устанавливаются
   * сервисом.
   *
   * @param dto DTO с данными записи времени
   * @return TimeEntry entity
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "durationSeconds", ignore = true)
  @Mapping(target = "cost", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  TimeEntry toEntity(TimeEntryDTO dto);

  /**
   * Преобразует TimeEntry entity в TimeEntryDTO.
   *
   * <p>Дополнительные поля (userName, taskTitle, projectName) должны заполняться в сервисном слое
   * при необходимости.
   *
   * @param timeEntry TimeEntry entity
   * @return TimeEntryDTO
   */
  @Mapping(target = "isActive", expression = "java(timeEntry.isActiveTimer())")
  TimeEntryDTO toDTO(TimeEntry timeEntry);

  /**
   * Частичное обновление TimeEntry entity из TimeEntryDTO.
   *
   * <p>Обновляет только не-null поля из DTO. Используется в операции редактирования.
   *
   * @param dto DTO с обновленными данными
   * @param timeEntry существующая TimeEntry entity для обновления
   */
  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "durationSeconds", ignore = true)
  @Mapping(target = "cost", ignore = true)
  void updateEntityFromDto(TimeEntryDTO dto, @MappingTarget TimeEntry timeEntry);
}
