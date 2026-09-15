package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.ShareLinkResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.ShareLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Share links",
        description = "Temporary, tokenized read-only links for sharing a patient's card")
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/patients/{id}/share-links")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Create a temporary share link for an assigned patient's card")
    public ShareLinkResponseDto createShareLink(
            Authentication authentication, @PathVariable Long id) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return shareLinkService.createShareLink(currentDoctor, id);
    }

    @GetMapping("/api/share-links/{token}/export")
    @Operation(summary = "Download the shared card PDF by token (no login required)")
    public ResponseEntity<byte[]> exportSharedCard(@PathVariable String token) {
        byte[] pdf = shareLinkService.exportSharedCard(token);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"medical-card.pdf\"")
                .body(pdf);
    }
}
