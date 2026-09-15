package com.vitaliy.medcard.dto;

import jakarta.validation.constraints.NotNull;

public record CareLinkRequestDto(
        @NotNull
        Long patientId
) {
}
