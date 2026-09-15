package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.ConditionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record ConditionRequestDto(
        @NotBlank
        String name,
        @NotNull(message = "Status must be ACTIVE or RESOLVED!")
        ConditionStatus status,
        @PastOrPresent(message = "Diagnosed date can't be in the future!")
        LocalDate diagnosedAt
) {
}
