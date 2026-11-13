package com.vitaliy.medcard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CareLinkRequestDto(
        @NotBlank
        @Email
        String doctorEmail
) {
}
