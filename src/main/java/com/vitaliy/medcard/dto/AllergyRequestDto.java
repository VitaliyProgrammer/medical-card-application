package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.AllergySeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AllergyRequestDto(
        @NotBlank
        String name,
        @NotNull(message = "Severity must be MILD, MODERATE or SEVERE!")
        AllergySeverity severity
) {
}
