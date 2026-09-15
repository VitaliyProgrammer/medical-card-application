package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.CareLinkResponseDto;
import com.vitaliy.medcard.model.CareLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CareLinkMapper {

    @Mapping(target = "doctorId", source = "doctor.id")
    @Mapping(target = "doctorFullName", source = "doctor.fullName")
    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "patientFullName", source = "patient.user.fullName")
    CareLinkResponseDto toDto(CareLink careLink);
}
