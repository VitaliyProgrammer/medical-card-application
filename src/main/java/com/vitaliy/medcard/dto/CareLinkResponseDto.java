package com.vitaliy.medcard.dto;

import java.time.LocalDateTime;

public record CareLinkResponseDto(
        Long id,
        Long doctorId,
        String doctorFullName,
        Long patientId,
        String patientFullName,
        LocalDateTime assignedAt
) {
}
