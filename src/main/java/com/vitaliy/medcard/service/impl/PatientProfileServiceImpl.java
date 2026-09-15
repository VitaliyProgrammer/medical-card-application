package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.PatientProfileRequestDto;
import com.vitaliy.medcard.dto.PatientProfileResponseDto;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.mapper.PatientProfileMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.service.PatientProfileService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;

    private final PatientProfileMapper patientProfileMapper;

    private final PatientAccessValidator patientAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponseDto getMyProfile(User currentUser) {

        PatientProfile profile = findByUserId(currentUser.getId());
        return patientProfileMapper.toDto(profile);
    }

    @Override
    @Audited(action = "UPDATE_PATIENT_PROFILE")
    public PatientProfileResponseDto updateMyProfile(
            User currentUser, PatientProfileRequestDto request) {

        PatientProfile profile = findByUserId(currentUser.getId());

        patientProfileMapper.updateFromDto(request, profile);
        profile.setUpdatedAt(LocalDateTime.now());

        return patientProfileMapper.toDto(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponseDto getProfileForDoctor(User currentDoctor, Long patientId) {

        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found: " + patientId + "!"));

        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        return patientProfileMapper.toDto(profile);
    }

    private PatientProfile findByUserId(Long userId) {

        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + userId + "!"));
    }
}
