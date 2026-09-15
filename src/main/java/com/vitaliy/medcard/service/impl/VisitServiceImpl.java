package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.VisitRequestDto;
import com.vitaliy.medcard.dto.VisitResponseDto;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.mapper.VisitMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.Visit;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.VisitRepository;
import com.vitaliy.medcard.service.VisitService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;

    private final PatientProfileRepository patientProfileRepository;

    private final VisitMapper visitMapper;

    private final PatientAccessValidator patientAccessValidator;

    @Override
    @Audited(action = "CREATE_VISIT")
    public VisitResponseDto createVisit(
            User currentDoctor, Long patientId, VisitRequestDto request) {
        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found: " + patientId + "!"));

        Visit visit = visitMapper.toEntity(request);
        visit.setPatient(patient);
        visit.setDoctor(currentDoctor);
        visit.setVisitDate(LocalDateTime.now());

        return visitMapper.toDto(visitRepository.save(visit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitResponseDto> getVisitsForDoctor(User currentDoctor, Long patientId) {

        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        return visitRepository.findByPatientIdOrderByVisitDateDesc(patientId).stream()
                .map(visitMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VisitResponseDto> getMyVisits(User currentUser) {

        PatientProfile patient = patientProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + currentUser.getId() + "!"));

        return visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId()).stream()
                .map(visitMapper::toDto)
                .toList();
    }
}
