package com.vitaliy.medcard.dto;

import java.time.LocalDateTime;

public record AuditEntryResponseDto(
        Long id,
        String performedByEmail,
        String action,
        String methodName,
        Long targetId,
        String outcome,
        LocalDateTime timestamp
) {
}
