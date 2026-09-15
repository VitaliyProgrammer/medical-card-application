package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.DocumentType;
import java.time.LocalDateTime;

public record DocumentResponseDto(
        Long id,
        DocumentType documentType,
        String originalFileName,
        String contentType,
        Long fileSize,
        String uploadedByFullName,
        LocalDateTime uploadedAt
) {
}
