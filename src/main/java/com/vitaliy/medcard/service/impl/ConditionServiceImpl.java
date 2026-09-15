package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.ConditionRequestDto;
import com.vitaliy.medcard.dto.ConditionResponseDto;
import com.vitaliy.medcard.exception.ConditionNotFoundException;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.mapper.ConditionMapper;
import com.vitaliy.medcard.model.Condition;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.ConditionRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.service.ConditionService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ConditionServiceImpl implements ConditionService {

    private final ConditionRepository conditionRepository;

    private final PatientProfileRepository patientProfileRepository;

    private final ConditionMapper conditionMapper;

    private final PatientAccessValidator patientAccessValidator;

    @Override
    @Transactional(readOnly = true)
    public List<ConditionResponseDto> getMyConditions(User currentUser) {

        PatientProfile patient = findPatientByUserId(currentUser.getId());

        return conditionRepository.findByPatientId(patient.getId()).stream()
                .map(conditionMapper::toDto)
                .toList();
    }

    @Override
    @Audited(action = "ADD_CONDITION")
    public ConditionResponseDto addCondition(User currentUser, ConditionRequestDto request) {

        PatientProfile patient = findPatientByUserId(currentUser.getId());

        Condition condition = conditionMapper.toEntity(request);
        condition.setPatient(patient);
        condition.setCreatedAt(LocalDateTime.now());

        return conditionMapper.toDto(conditionRepository.save(condition));
    }

    @Override
    @Audited(action = "DELETE_CONDITION")
    public void deleteMyCondition(User currentUser, Long conditionId) {

        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ConditionNotFoundException(
                        "Condition not found: " + conditionId + "!"));

        if (!condition.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("This condition doesn't belong to you!");
        }

        conditionRepository.delete(condition);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConditionResponseDto> getConditionsForDoctor(User currentDoctor, Long patientId) {

        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        return conditionRepository.findByPatientId(patientId).stream()
                .map(conditionMapper::toDto)
                .toList();
    }

    private PatientProfile findPatientByUserId(Long userId) {
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + userId + "!"));
    }
}
