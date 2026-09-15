package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.ConditionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConditionResponseDto(
        Long id,
        String name,
        ConditionStatus status,
        LocalDate diagnosedAt,
        LocalDateTime createdAt
) {
}
