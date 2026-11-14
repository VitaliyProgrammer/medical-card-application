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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Care links",
        description = "Patients grant and revoke doctors' access to their medical card")
public class CareLinkController {

    private final CareLinkService careLinkService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/patients/me/care-links")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Grant a doctor access to my card by the doctor's email")
    public CareLinkResponseDto grantDoctorAccess(
            Authentication authentication, @RequestBody @Valid CareLinkRequestDto request) {
        User currentPatient = currentUserService.getCurrentUser(authentication);
        return careLinkService.grantDoctorAccess(currentPatient, request);
    }

    @GetMapping("/api/patients/me/care-links")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List doctors who have access to my card")
    public List<CareLinkResponseDto> getMyDoctors(Authentication authentication) {
        User currentPatient = currentUserService.getCurrentUser(authentication);
        return careLinkService.getMyDoctors(currentPatient);
    }

    @DeleteMapping("/api/patients/me/care-links/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Revoke a doctor's access to my card")
    public void revokeDoctorAccess(Authentication authentication, @PathVariable Long id) {
        User currentPatient = currentUserService.getCurrentUser(authentication);
        careLinkService.revokeDoctorAccess(currentPatient, id);
    }

    @GetMapping("/api/care-links/my-patients")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "List and search patients who granted me access",
            description = "Supports pagination, sorting and searching by patient full name")
    public Page<PatientSummaryDto> getMyPatients(
            Authentication authentication,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return careLinkService.searchMyPatients(currentDoctor, search, pageable);
    }
}
