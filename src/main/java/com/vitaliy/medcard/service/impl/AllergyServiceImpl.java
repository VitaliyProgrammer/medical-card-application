package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.AllergyRequestDto;
import com.vitaliy.medcard.dto.AllergyResponseDto;
import com.vitaliy.medcard.exception.AllergyNotFoundException;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.mapper.AllergyMapper;
import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.AllergyRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.service.AllergyService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AllergyServiceImpl implements AllergyService {

    private final AllergyRepository allergyRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final AllergyMapper allergyMapper;
    private final PatientAccessValidator patientAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public List<AllergyResponseDto> getMyAllergies(User currentUser) {
        PatientProfile patient = findPatientByUserId(currentUser.getId());

        return allergyRepository.findByPatientId(patient.getId()).stream()
                .map(allergyMapper::toDto)
                .toList();
    }

    @Override
    @Audited(action = "ADD_ALLERGY")
    public AllergyResponseDto addAllergy(User currentUser, AllergyRequestDto request) {
        PatientProfile patient = findPatientByUserId(currentUser.getId());

        Allergy allergy = allergyMapper.toEntity(request);
        allergy.setPatient(patient);
        allergy.setCreatedAt(LocalDateTime.now());

        return allergyMapper.toDto(allergyRepository.save(allergy));
    }

    @Override
    @Audited(action = "DELETE_ALLERGY")
    public void deleteMyAllergy(User currentUser, Long allergyId) {
        Allergy allergy = allergyRepository.findById(allergyId)
                .orElseThrow(() -> new AllergyNotFoundException(
                        "Allergy not found: " + allergyId + "!"));

        if (!allergy.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("This allergy doesn't belong to you!");
        }

        allergyRepository.delete(allergy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AllergyResponseDto> getAllergiesForDoctor(User currentDoctor, Long patientId) {
        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        return allergyRepository.findByPatientId(patientId).stream()
                .map(allergyMapper::toDto)
                .toList();
    }

    private PatientProfile findPatientByUserId(Long userId) {
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + userId + "!"));
    }
}
