package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.ReminderRequestDto;
import com.vitaliy.medcard.dto.ReminderResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.ReminderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reminders", description = "Patient's own medication and appointment reminders")
public class ReminderController {

    private final ReminderService reminderService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/patients/me/reminders")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my own reminders")
    public List<ReminderResponseDto> getMyReminders(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return reminderService.getMyReminders(currentUser);
    }

    @PostMapping("/api/patients/me/reminders")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Add a reminder to my own card")
    public ReminderResponseDto addReminder(
            Authentication authentication, @RequestBody @Valid ReminderRequestDto request) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return reminderService.addReminder(currentUser, request);
    }

    @DeleteMapping("/api/patients/me/reminders/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Delete one of my own reminders")
    public void deleteMyReminder(Authentication authentication, @PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        reminderService.deleteMyReminder(currentUser, id);
    }
}
