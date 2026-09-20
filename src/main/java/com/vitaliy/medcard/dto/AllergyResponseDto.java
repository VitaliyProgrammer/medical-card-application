package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.AllergySeverity;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AllergyResponseDto(
        Long id,
        String name,
        AllergySeverity severity,
        LocalDate diagnosedAt,
        LocalDateTime createdAt
) {
}
