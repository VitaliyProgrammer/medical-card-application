package com.vitaliy.medcard.dto;

import java.time.LocalDate;

public record PatientProfileResponseDto(
        Long id,
        String fullName,
        String email,
        LocalDate dateOfBirth,
        Integer age,
        String bloodGroup,
        String emergencyContactName,
        String emergencyContactPhone
) {
}
