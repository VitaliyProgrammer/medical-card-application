package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.Condition;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConditionRepository extends JpaRepository<Condition, Long> {

    List<Condition> findByPatientId(Long patientId);
}
