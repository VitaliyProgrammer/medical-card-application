package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.AllergySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public record AllergyRequestDto(
        @NotBlank
        String name,
        @NotNull(message = "Severity must be MILD, MODERATE or SEVERE!")
        AllergySeverity severity,
        @PastOrPresent(message = "Diagnosed date can't be in the future!")
        LocalDate diagnosedAt
) {
}
