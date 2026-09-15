package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.Visit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    List<Visit> findByPatientIdOrderByVisitDateDesc(Long patientId);
}
