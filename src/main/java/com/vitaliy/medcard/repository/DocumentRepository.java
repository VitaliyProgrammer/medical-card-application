package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.Document;
import com.vitaliy.medcard.model.status.DocumentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdAndPatientId(Long id, Long patientId);

    @EntityGraph(attributePaths = "uploadedBy")
    List<Document> findByPatientId(Long patientId);

    @EntityGraph(attributePaths = "uploadedBy")
    List<Document> findByPatientIdAndDocumentType(Long patientId, DocumentType documentType);
}
