package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.VisitRequestDto;
import com.vitaliy.medcard.dto.VisitResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Visits", description = "Append-only visit / encounter records")
public class VisitController {

    private final VisitService visitService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/patients/{id}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Record a new visit for an assigned patient")
    public VisitResponseDto createVisit(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody @Valid VisitRequestDto request) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return visitService.createVisit(currentDoctor, id, request);
    }

    @GetMapping("/api/patients/{id}/visits")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "List visit history for an assigned patient")
    public List<VisitResponseDto> getPatientVisits(
            Authentication authentication, @PathVariable Long id) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return visitService.getVisitsForDoctor(currentDoctor, id);
    }

    @GetMapping("/api/patients/me/visits")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my own visit history")
    public List<VisitResponseDto> getMyVisits(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return visitService.getMyVisits(currentUser);
    }
}
