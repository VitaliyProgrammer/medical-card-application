package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.ConditionRequestDto;
import com.vitaliy.medcard.dto.ConditionResponseDto;
import com.vitaliy.medcard.model.Condition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConditionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Condition toEntity(ConditionRequestDto dto);

    ConditionResponseDto toDto(Condition condition);
}
