package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.PatientProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {

    Optional<PatientProfile> findByUserId(Long userId);
}
