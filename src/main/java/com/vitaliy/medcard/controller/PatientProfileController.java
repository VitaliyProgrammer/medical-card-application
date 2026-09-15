package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.PatientProfileRequestDto;
import com.vitaliy.medcard.dto.PatientProfileResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.PatientProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patient card", description = "Reading and editing the patient's medical card")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get my own medical card")
    public PatientProfileResponseDto getMyProfile(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return patientProfileService.getMyProfile(currentUser);
    }

    @PutMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Update my own medical card")
    public PatientProfileResponseDto updateMyProfile(
            Authentication authentication,
            @RequestBody @Valid PatientProfileRequestDto request) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return patientProfileService.updateMyProfile(currentUser, request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Get a patient's card",
            description = "Only allowed for a doctor assigned to this patient via CareLink")
    public PatientProfileResponseDto getPatientProfile(
            Authentication authentication, @PathVariable Long id) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return patientProfileService.getProfileForDoctor(currentDoctor, id);
    }
}
