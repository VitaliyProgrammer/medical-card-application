package com.vitaliy.medcard.dto;

import jakarta.validation.constraints.NotBlank;

public record VisitRequestDto(
        String bloodPressure,
        Integer heartRate,
        Double temperatureCelsius,
        Double weightKg,
        @NotBlank(message = "Diagnosis can't be blank!")
        String diagnosis,
        String notes
) {
}
