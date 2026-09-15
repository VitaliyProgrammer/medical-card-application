package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.ConditionRequestDto;
import com.vitaliy.medcard.dto.ConditionResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.ConditionService;
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
@Tag(name = "Conditions", description = "Patient chronic conditions / diagnoses")
public class ConditionController {

    private final ConditionService conditionService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/patients/me/conditions")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my own chronic conditions")
    public List<ConditionResponseDto> getMyConditions(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return conditionService.getMyConditions(currentUser);
    }

    @PostMapping("/api/patients/me/conditions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Add a chronic condition to my own card")
    public ConditionResponseDto addCondition(
            Authentication authentication, @RequestBody @Valid ConditionRequestDto request) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return conditionService.addCondition(currentUser, request);
    }

    @DeleteMapping("/api/patients/me/conditions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Remove a chronic condition from my own card")
    public void deleteMyCondition(Authentication authentication, @PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        conditionService.deleteMyCondition(currentUser, id);
    }

    @GetMapping("/api/patients/{id}/conditions")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "List chronic conditions for an assigned patient")
    public List<ConditionResponseDto> getPatientConditions(
            Authentication authentication, @PathVariable Long id) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return conditionService.getConditionsForDoctor(currentDoctor, id);
    }
}
