package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.Condition;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.Visit;
import com.vitaliy.medcard.repository.AllergyRepository;
import com.vitaliy.medcard.repository.ConditionRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.VisitRepository;
import com.vitaliy.medcard.service.PatientCardExportService;
import com.vitaliy.medcard.service.PdfExportService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PatientCardExportServiceImpl implements PatientCardExportService {

    private final PatientProfileRepository patientProfileRepository;
    private final AllergyRepository allergyRepository;
    private final ConditionRepository conditionRepository;
    private final VisitRepository visitRepository;
    private final PdfExportService pdfExportService;
    private final PatientAccessValidator patientAccessValidator;

    @Override
    public byte[] exportMyCard(User currentUser) {
        PatientProfile patient = patientProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + currentUser.getId() + "!"));

        return exportCardById(patient.getId());
    }

    @Override
    public byte[] exportCardForDoctor(User currentDoctor, Long patientId) {
        patientAccessValidator.validateDoctorAccess(currentDoctor, patientId);

        return exportCardById(patientId);
    }

    @Override
    public byte[] exportCardById(Long patientId) {
        PatientProfile patient = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found: " + patientId + "!"));

        List<Allergy> allergies = allergyRepository.findByPatientId(patientId);
        List<Condition> conditions = conditionRepository.findByPatientId(patientId);
        List<Visit> visits = visitRepository.findByPatientIdOrderByVisitDateDesc(patientId);

        return pdfExportService.renderPatientCard(patient, allergies, conditions, visits);
    }
}
