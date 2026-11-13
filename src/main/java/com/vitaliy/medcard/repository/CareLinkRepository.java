package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.CareLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CareLinkRepository
        extends JpaRepository<CareLink, Long>, JpaSpecificationExecutor<CareLink> {

    boolean existsByDoctorIdAndPatientId(Long doctorId, Long patientId);

    List<CareLink> findByPatientId(Long patientId);

    Optional<CareLink> findByIdAndPatientId(Long id, Long patientId);
}
