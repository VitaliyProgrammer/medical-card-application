package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.Document;
import com.vitaliy.medcard.model.status.DocumentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByPatientId(Long patientId);

    List<Document> findByPatientIdAndDocumentType(Long patientId, DocumentType documentType);
}
