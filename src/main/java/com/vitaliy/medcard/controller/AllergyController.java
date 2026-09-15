package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.AllergyRequestDto;
import com.vitaliy.medcard.dto.AllergyResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.AllergyService;
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
@Tag(name = "Allergies", description = "Patient-reported allergies")
public class AllergyController {

    private final AllergyService allergyService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/patients/me/allergies")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my own allergies")
    public List<AllergyResponseDto> getMyAllergies(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return allergyService.getMyAllergies(currentUser);
    }

    @PostMapping("/api/patients/me/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Add an allergy to my own card")
    public AllergyResponseDto addAllergy(
            Authentication authentication, @RequestBody @Valid AllergyRequestDto request) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return allergyService.addAllergy(currentUser, request);
    }

    @DeleteMapping("/api/patients/me/allergies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Remove an allergy from my own card")
    public void deleteMyAllergy(Authentication authentication, @PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        allergyService.deleteMyAllergy(currentUser, id);
    }

    @GetMapping("/api/patients/{id}/allergies")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "List allergies for an assigned patient")
    public List<AllergyResponseDto> getPatientAllergies(
            Authentication authentication, @PathVariable Long id) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return allergyService.getAllergiesForDoctor(currentDoctor, id);
    }
}
