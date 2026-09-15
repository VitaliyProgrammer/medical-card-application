package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.AllergyRequestDto;
import com.vitaliy.medcard.dto.AllergyResponseDto;
import com.vitaliy.medcard.model.Allergy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AllergyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Allergy toEntity(AllergyRequestDto dto);

    AllergyResponseDto toDto(Allergy allergy);
}
