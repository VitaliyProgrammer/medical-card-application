package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.DocumentFileDto;
import com.vitaliy.medcard.dto.DocumentResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.DocumentType;
import com.vitaliy.medcard.security.CurrentUserService;
import com.vitaliy.medcard.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents",
        description = "Uploaded medical documents: lab results, scans, prescriptions")
public class DocumentController {

    private final DocumentService documentService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/api/patients/me/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Upload a document to my own card")
    public DocumentResponseDto uploadMyDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return documentService.uploadMyDocument(currentUser, file, documentType);
    }

    @GetMapping("/api/patients/me/documents")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "List my own documents, optionally filtered by type")
    public List<DocumentResponseDto> getMyDocuments(
            Authentication authentication,
            @RequestParam(required = false) DocumentType type) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return documentService.getMyDocuments(currentUser, type);
    }

    @GetMapping("/api/patients/me/documents/{id}/download")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Download one of my own documents")
    public ResponseEntity<Resource> downloadMyDocument(
            Authentication authentication, @PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        return buildDownloadResponse(documentService.downloadMyDocument(currentUser, id));
    }

    @DeleteMapping("/api/patients/me/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Delete one of my own documents")
    public void deleteMyDocument(Authentication authentication, @PathVariable Long id) {
        User currentUser = currentUserService.getCurrentUser(authentication);
        documentService.deleteMyDocument(currentUser, id);
    }

    @GetMapping("/api/patients/{id}/documents")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "List documents for an assigned patient")
    public List<DocumentResponseDto> getPatientDocuments(
            Authentication authentication, @PathVariable Long id,
            @RequestParam(required = false) DocumentType type) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return documentService.getDocumentsForDoctor(currentDoctor, id, type);
    }

    @GetMapping("/api/patients/{id}/documents/{documentId}/download")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    @Operation(summary = "Download a document for an assigned patient")
    public ResponseEntity<Resource> downloadPatientDocument(
            Authentication authentication,
            @PathVariable Long id,
            @PathVariable Long documentId) {
        User currentDoctor = currentUserService.getCurrentUser(authentication);
        return buildDownloadResponse(
                documentService.downloadDocumentForDoctor(currentDoctor, id, documentId));
    }

    private ResponseEntity<Resource> buildDownloadResponse(DocumentFileDto fileDto) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileDto.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileDto.fileName() + "\"")
                .body(fileDto.resource());
    }
}
