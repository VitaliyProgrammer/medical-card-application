package com.vitaliy.medcard.mapper;

import com.vitaliy.medcard.dto.PatientProfileRequestDto;
import com.vitaliy.medcard.dto.PatientProfileResponseDto;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.util.AgeCalculator;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", imports = AgeCalculator.class)
public interface PatientProfileMapper {

    @Mapping(target = "fullName", source = "user.fullName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "age",
            expression = "java(AgeCalculator.calculateAge(patientProfile.getDateOfBirth()))")
    PatientProfileResponseDto toDto(PatientProfile patientProfile);

    void updateFromDto(PatientProfileRequestDto dto, @MappingTarget PatientProfile entity);
}
