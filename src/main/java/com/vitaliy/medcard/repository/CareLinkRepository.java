package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.CareLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CareLinkRepository
        extends JpaRepository<CareLink, Long>, JpaSpecificationExecutor<CareLink> {

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);

    @EntityGraph(attributePaths = {"doctor", "patient", "patient.user"})
    List<CareLink> findByPatientId(Long patientId);

    Optional<CareLink> findByIdAndPatientId(Long id, Long patientId);

    @Override
    @EntityGraph(attributePaths = {"patient", "patient.user"})
    Page<CareLink> findAll(Specification<CareLink> specification, Pageable pageable);
}
