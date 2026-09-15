package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.AllergyRepository;
import com.vitaliy.medcard.repository.ConditionRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.VisitRepository;
import com.vitaliy.medcard.service.PdfExportService;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientCardExportServiceImplTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private PdfExportService pdfExportService;

    @Mock
    private PatientAccessValidator patientAccessValidator;

    @InjectMocks
    private PatientCardExportServiceImpl patientCardExportService;

    private User patientUser;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(1L);

        patient = new PatientProfile();
        patient.setId(10L);
        patient.setUser(patientUser);
    }

    @Test
    @DisplayName("exportMyCard: assembles the patient's data and renders a PDF")
    void exportMyCard_success() {
        byte[] pdfBytes = {1, 2, 3};

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(allergyRepository.findByPatientId(patient.getId())).thenReturn(List.of());
        when(conditionRepository.findByPatientId(patient.getId())).thenReturn(List.of());
        when(visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId()))
                .thenReturn(List.of());
        when(pdfExportService.renderPatientCard(patient, List.of(), List.of(), List.of()))
                .thenReturn(pdfBytes);

        byte[] result = patientCardExportService.exportMyCard(patientUser);

        assertThat(result).isEqualTo(pdfBytes);
    }

    @Test
    @DisplayName("exportMyCard: throws when the caller has no card yet")
    void exportMyCard_noProfile() {
        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(PatientProfileNotFoundException.class,
                () -> patientCardExportService.exportMyCard(patientUser));
    }

    @Test
    @DisplayName("exportCardForDoctor: rejects an unassigned doctor before touching the data")
    void exportCardForDoctor_notAssigned() {
        User doctor = new User();
        doctor.setId(2L);

        doThrow(new ForbiddenActionException("You are not assigned to this patient's care!"))
                .when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());

        assertThrows(ForbiddenActionException.class,
                () -> patientCardExportService.exportCardForDoctor(doctor, patient.getId()));
    }

    @Test
    @DisplayName("exportCardForDoctor: renders the PDF once access is validated")
    void exportCardForDoctor_success() {
        User doctor = new User();
        doctor.setId(2L);
        byte[] pdfBytes = {1, 2, 3};

        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(allergyRepository.findByPatientId(patient.getId())).thenReturn(List.of());
        when(conditionRepository.findByPatientId(patient.getId())).thenReturn(List.of());
        when(visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId()))
                .thenReturn(List.of());
        when(pdfExportService.renderPatientCard(any(), any(), any(), any())).thenReturn(pdfBytes);

        byte[] result = patientCardExportService.exportCardForDoctor(doctor, patient.getId());

        assertThat(result).isEqualTo(pdfBytes);
    }
}
