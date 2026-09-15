package com.vitaliy.medcard.dto;

import java.time.LocalDateTime;

public record VisitResponseDto(
        Long id,
        String doctorFullName,
        LocalDateTime visitDate,
        String bloodPressure,
        Integer heartRate,
        Double temperatureCelsius,
        Double weightKg,
        String diagnosis,
        String notes
) {
}
