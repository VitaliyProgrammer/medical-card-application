package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.ReminderType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ReminderRequestDto(
        @NotBlank(message = "Title can't be blank!")
        String title,
        @NotNull(message = "Type must be MEDICATION or APPOINTMENT!")
        ReminderType type,
        @NotNull(message = "Due date is required!")
        @FutureOrPresent(message = "Due date can't be in the past!")
        LocalDateTime dueAt
) {
}
