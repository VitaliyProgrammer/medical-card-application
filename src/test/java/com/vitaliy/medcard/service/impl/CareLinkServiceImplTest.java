package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.CareLinkRequestDto;
import com.vitaliy.medcard.dto.CareLinkResponseDto;
import com.vitaliy.medcard.dto.PatientSummaryDto;
import com.vitaliy.medcard.exception.CareLinkAlreadyExistsException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.mapper.CareLinkMapper;
import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.status.AllergySeverity;
import com.vitaliy.medcard.model.CareLink;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.AllergyRepository;
import com.vitaliy.medcard.repository.CareLinkRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class CareLinkServiceImplTest {

    @Mock
    private CareLinkRepository careLinkRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private CareLinkMapper careLinkMapper;

    @InjectMocks
    private CareLinkServiceImpl careLinkService;

    private User doctor;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(1L);

        User patientUser = new User();
        patientUser.setId(3L);
        patientUser.setFullName("Jane Patient");

        patient = new PatientProfile();
        patient.setId(2L);
        patient.setUser(patientUser);
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("assignPatientToDoctor: creates a new CareLink")
    void assignPatientToDoctor_success() {
        CareLinkRequestDto request = new CareLinkRequestDto(patient.getId());
        CareLinkResponseDto responseDto = new CareLinkResponseDto(
                100L, doctor.getId(), "Dr. House", patient.getId(), "Jane Patient",
                LocalDateTime.now());

        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(careLinkRepository.existsByDoctorIdAndPatientId(doctor.getId(), patient.getId()))
                .thenReturn(false);
        when(careLinkRepository.save(any(CareLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(careLinkMapper.toDto(any(CareLink.class))).thenReturn(responseDto);

        CareLinkResponseDto result = careLinkService.assignPatientToDoctor(doctor, request);

        assertThat(result).isEqualTo(responseDto);
        verify(careLinkRepository).save(any(CareLink.class));
    }

    @Test
    @DisplayName("assignPatientToDoctor: rejects a duplicate assignment")
    void assignPatientToDoctor_duplicate() {
        CareLinkRequestDto request = new CareLinkRequestDto(patient.getId());

        when(patientProfileRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(careLinkRepository.existsByDoctorIdAndPatientId(doctor.getId(), patient.getId()))
                .thenReturn(true);

        assertThrows(CareLinkAlreadyExistsException.class,
                () -> careLinkService.assignPatientToDoctor(doctor, request));

        verify(careLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("assignPatientToDoctor: throws when the patient doesn't exist")
    void assignPatientToDoctor_patientNotFound() {
        CareLinkRequestDto request = new CareLinkRequestDto(999L);

        when(patientProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PatientProfileNotFoundException.class,
                () -> careLinkService.assignPatientToDoctor(doctor, request));
    }

    @Test
    @DisplayName("searchMyPatients: returns assigned patients with age and allergy names")
    void searchMyPatients_success() {
        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patient);

        Allergy allergy = new Allergy();
        allergy.setPatient(patient);
        allergy.setName("Penicillin");
        allergy.setSeverity(AllergySeverity.SEVERE);

        Pageable pageable = PageRequest.of(0, 10);
        Page<CareLink> careLinkPage = new PageImpl<>(List.of(careLink), pageable, 1);

        when(careLinkRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(careLinkPage);
        when(allergyRepository.findByPatientIdIn(List.of(patient.getId())))
                .thenReturn(List.of(allergy));

        Page<PatientSummaryDto> result =
                careLinkService.searchMyPatients(doctor, "Jane", pageable);

        assertThat(result.getContent()).hasSize(1);
        PatientSummaryDto summary = result.getContent().get(0);
        assertThat(summary.fullName()).isEqualTo("Jane Patient");
        assertThat(summary.age()).isNotNull();
        assertThat(summary.allergies()).containsExactly("Penicillin");
    }
}
