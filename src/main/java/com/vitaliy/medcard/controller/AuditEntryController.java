package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.AuditEntryResponseDto;
import com.vitaliy.medcard.service.AuditEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-entries")
@RequiredArgsConstructor
@Tag(name = "Audit log", description = "Who changed what and when - admin only")
public class AuditEntryController {

    private final AuditEntryService auditEntryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List audit log entries, paginated")
    public Page<AuditEntryResponseDto> getAuditEntries(Pageable pageable) {
        return auditEntryService.findAll(pageable);
    }
}
