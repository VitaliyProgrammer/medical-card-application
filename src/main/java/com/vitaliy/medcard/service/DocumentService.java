package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.DocumentFileDto;
import com.vitaliy.medcard.dto.DocumentResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.DocumentType;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentResponseDto uploadMyDocument(
            User currentUser, MultipartFile file, DocumentType documentType);

    List<DocumentResponseDto> getMyDocuments(User currentUser, DocumentType typeFilter);

    void deleteMyDocument(User currentUser, Long documentId);

    DocumentFileDto downloadMyDocument(User currentUser, Long documentId);

    List<DocumentResponseDto> getDocumentsForDoctor(
            User currentDoctor, Long patientId, DocumentType typeFilter);

    DocumentFileDto downloadDocumentForDoctor(User currentDoctor, Long patientId, Long documentId);
}
