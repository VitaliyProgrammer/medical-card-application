package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.ReminderType;
import java.time.LocalDateTime;

public record ReminderResponseDto(
        Long id,
        String title,
        ReminderType type,
        LocalDateTime dueAt,
        boolean notified
) {
}
