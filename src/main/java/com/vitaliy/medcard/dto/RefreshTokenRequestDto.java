package com.vitaliy.medcard.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(
        @NotBlank(message = "Refresh token must not be blank!")
        String refreshToken
) {
}
