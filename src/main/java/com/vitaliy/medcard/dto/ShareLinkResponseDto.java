package com.vitaliy.medcard.dto;

import java.time.LocalDateTime;

public record ShareLinkResponseDto(
        Long id,
        String token,
        LocalDateTime expiresAt
) {
}
