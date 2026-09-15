package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.VisitRequestDto;
import com.vitaliy.medcard.dto.VisitResponseDto;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.mapper.VisitMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.Visit;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.VisitRepository;
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
class VisitServiceImplTest {

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private VisitMapper visitMapper;

    @Mock
    private PatientAccessValidator patientAccessValidator;

    @InjectMocks
    private VisitServiceImpl visitService;

    private User doctor;

    private User patientUser;

    private PatientProfile patient;

    private Visit visit;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(1L);

        patientUser = new User();
        patientUser.setId(2L);

        patient = new PatientProfile();
        patient.setId(10L);
        patient.setUser(patientUser);

        visit = new Visit();
        visit.setId(500L);
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setDiagnosis("Common cold");
    }

    @Test
    @DisplayName("createVisit: records a new append-only visit for an assigned patient")
    void createVisit_success() {
        VisitRequestDto request =
                new VisitRequestDto("120/80", 72, 36.6, 70.0, "Common cold", "Rest advised");
        VisitResponseDto responseDto = new VisitResponseDto(
                500L, "Dr. House", null, "120/80", 72, 36.6, 70.0, "Common cold", "Rest advised");

        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());
        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(visitMapper.toEntity(request)).thenReturn(visit);
        when(visitRepository.save(visit)).thenReturn(visit);
        when(visitMapper.toDto(visit)).thenReturn(responseDto);

        VisitResponseDto result = visitService.createVisit(doctor, patient.getId(), request);

        assertThat(result).isEqualTo(responseDto);
        assertThat(visit.getVisitDate()).isNotNull();
        assertThat(visit.getDoctor()).isEqualTo(doctor);
    }

    @Test
    @DisplayName("createVisit: rejects a doctor who isn't assigned to this patient")
    void createVisit_notAssigned() {
        VisitRequestDto request =
                new VisitRequestDto(null, null, null, null, "Common cold", null);

        doThrow(new ForbiddenActionException("You are not assigned to this patient's care!"))
                .when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());

        assertThrows(ForbiddenActionException.class,
                () -> visitService.createVisit(doctor, patient.getId(), request));
    }

    @Test
    @DisplayName("getMyVisits: returns the caller's visit history ordered newest first")
    void getMyVisits_success() {
        VisitResponseDto responseDto = new VisitResponseDto(
                500L, "Dr. House", null, null, null, null, null, "Common cold", null);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(visitRepository.findByPatientIdOrderByVisitDateDesc(patient.getId()))
                .thenReturn(List.of(visit));
        when(visitMapper.toDto(visit)).thenReturn(responseDto);

        List<VisitResponseDto> result = visitService.getMyVisits(patientUser);

        assertThat(result).containsExactly(responseDto);
    }

    @Test
    @DisplayName("getMyVisits: throws when the caller has no card yet")
    void getMyVisits_noProfile() {
        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(PatientProfileNotFoundException.class,
                () -> visitService.getMyVisits(patientUser));
    }
}
