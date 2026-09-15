package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.ReminderRequestDto;
import com.vitaliy.medcard.dto.ReminderResponseDto;
import com.vitaliy.medcard.model.Reminder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReminderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "notified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Reminder toEntity(ReminderRequestDto dto);

    ReminderResponseDto toDto(Reminder reminder);
}
