package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.UserRole;

public record UserRegistrationResponseDto(
        Long id,
        String email,
        String fullName,
        UserRole role
) {
}
