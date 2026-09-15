package com.vitaliy.medcard.dto;

import org.springframework.core.io.Resource;

public record DocumentFileDto(
        Resource resource,
        String fileName,
        String contentType
) {
}
