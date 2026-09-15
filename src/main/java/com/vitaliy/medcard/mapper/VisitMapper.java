package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.VisitRequestDto;
import com.vitaliy.medcard.dto.VisitResponseDto;
import com.vitaliy.medcard.model.Visit;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VisitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "doctor", ignore = true)
    @Mapping(target = "visitDate", ignore = true)
    Visit toEntity(VisitRequestDto dto);

    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    VisitResponseDto toDto(Visit visit);
}
