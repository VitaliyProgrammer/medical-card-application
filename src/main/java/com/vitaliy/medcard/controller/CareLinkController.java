package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.CareLinkRequestDto;
import com.vitaliy.medcard.dto.CareLinkResponseDto;
import com.vitaliy.medcard.dto.PatientSummaryDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.CareLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/care-links")
@RequiredArgsConstructor
@Tag(name = "Care links", description = "Assigning patients to a doctor's care")
public class CareLinkController {

    private final CareLinkService careLinkService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Assign a patient to the current doctor's care")
    public CareLinkResponseDto assignPatient(
            Authentication authentication, @RequestBody @Valid CareLinkRequestDto request) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return careLinkService.assignPatientToDoctor(currentDoctor, request);
    }

    @GetMapping("/my-patients")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "List and search my assigned patients",
            description = "Supports pagination, sorting and searching by patient full name")
    public Page<PatientSummaryDto> getMyPatients(
            Authentication authentication,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return careLinkService.searchMyPatients(currentDoctor, search, pageable);
    }
}
