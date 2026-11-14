package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PatientProfileLookup {

    private final PatientProfileRepository patientProfileRepository;

    PatientProfile getByUserId(Long userId) {
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + userId + "!"));
    }
}
