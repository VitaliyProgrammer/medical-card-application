package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.PatientCardExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Card export", description = "PDF export of the medical card")
public class PatientCardExportController {

    private static final String EXPORT_FILENAME = "medical-card.pdf";

    private final PatientCardExportService patientCardExportService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/patients/me/export")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Export my own medical card as a PDF")
    public ResponseEntity<byte[]> exportMyCard(Authentication authentication) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return pdfResponse(patientCardExportService.exportMyCard(currentUser));
    }

    @GetMapping("/api/patients/{id}/export")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Export an assigned patient's medical card as a PDF")
    public ResponseEntity<byte[]> exportPatientCard(
            Authentication authentication, @PathVariable Long id) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return pdfResponse(patientCardExportService.exportCardForDoctor(currentDoctor, id));
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + EXPORT_FILENAME + "\"")
                .body(pdf);
    }
}
