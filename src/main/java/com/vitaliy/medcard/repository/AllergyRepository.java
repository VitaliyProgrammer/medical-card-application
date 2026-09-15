package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.Allergy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergyRepository extends JpaRepository<Allergy, Long> {

    List<Allergy> findByPatientId(Long patientId);

    List<Allergy> findByPatientIdIn(List<Long> patientIds);
}
