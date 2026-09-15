package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.DocumentFileDto;
import com.vitaliy.medcard.dto.DocumentResponseDto;
import com.vitaliy.medcard.exception.DocumentNotFoundException;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.exception.UnsupportedFileTypeException;
import com.vitaliy.medcard.mapper.DocumentMapper;
import com.vitaliy.medcard.model.Document;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.DocumentType;
import com.vitaliy.medcard.repository.DocumentRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.service.DocumentService;
import com.vitaliy.medcard.service.FileStorageService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentServiceImpl implements DocumentService {

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;
    private final PatientAccessValidator patientAccessValidator;

    @Override
    @Audited(action = "UPLOAD_DOCUMENT")
    public DocumentResponseDto uploadMyDocument(
            User currentUser, MultipartFile file, DocumentType documentType) {
        PatientProfile patient = findPatientByUserId(currentUser.getId());

        return storeDocument(patient, currentUser, file, documentType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getMyDocuments(User currentUser, DocumentType typeFilter) {
        PatientProfile patient = findPatientByUserId(currentUser.getId());

        return listDocuments(patient.getId(), typeFilter);
    }

    @Override
    @Audited(action = "DELETE_DOCUMENT")
    public void deleteMyDocument(User currentUser, Long documentId) {
        Document document = getOwnedDocument(currentUser, documentId);

        fileStorageService.delete(document.getStoredFileName());
        documentRepository.delete(document);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentFileDto downloadMyDocument(User currentUser, Long documentId) {
        Document document = getOwnedDocument(currentUser, documentId);

        return toFileDto(document);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDto> getDocumentsForDoctor(
            User currentDoctor, Long patientId, DocumentType typeFilter) {
        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        return listDocuments(patientId, typeFilter);
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentFileDto downloadDocumentForDoctor(
            User currentDoctor, Long patientId, Long documentId) {
        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found: " + documentId + "!"));

        return toFileDto(document);
    }

    private DocumentResponseDto storeDocument(
            PatientProfile patient, User uploader, MultipartFile file, DocumentType documentType) {
        validateFile(file);

        String storedFileName = fileStorageService.store(file);

        Document document = new Document();
        document.setPatient(patient);
        document.setUploadedBy(uploader);
        document.setDocumentType(documentType);
        document.setOriginalFileName(StringUtils.cleanPath(file.getOriginalFilename()));
        document.setStoredFileName(storedFileName);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setUploadedAt(LocalDateTime.now());

        return documentMapper.toDto(documentRepository.save(document));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UnsupportedFileTypeException("Uploaded file is empty!");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new UnsupportedFileTypeException("File exceeds the 10 MB size limit!");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type: " + file.getContentType() + "!");
        }
    }

    private List<DocumentResponseDto> listDocuments(Long patientId, DocumentType typeFilter) {
        List<Document> documents = typeFilter == null
                ? documentRepository.findByPatientId(patientId)
                : documentRepository.findByPatientIdAndDocumentType(patientId, typeFilter);

        return documents.stream().map(documentMapper::toDto).toList();
    }

    private Document getOwnedDocument(User currentUser, Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found: " + documentId + "!"));

        if (!document.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("This document doesn't belong to you!");
        }

        return document;
    }

    private DocumentFileDto toFileDto(Document document) {
        Resource resource = fileStorageService.load(document.getStoredFileName());
        return new DocumentFileDto(
                resource, document.getOriginalFileName(), document.getContentType());
    }

    private PatientProfile findPatientByUserId(Long userId) {
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + userId + "!"));
    }
}
