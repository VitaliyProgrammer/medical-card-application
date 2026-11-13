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
import com.vitaliy.medcard.exception.CareLinkNotFoundException;
import com.vitaliy.medcard.exception.UserNotFoundException;
import com.vitaliy.medcard.mapper.CareLinkMapper;
import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.CareLink;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.AllergySeverity;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.AllergyRepository;
import com.vitaliy.medcard.repository.CareLinkRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    private static final String DOCTOR_EMAIL = "doctor@test.com";

    @Mock
    private CareLinkRepository careLinkRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private CareLinkMapper careLinkMapper;

    @InjectMocks
    private CareLinkServiceImpl careLinkService;

    private User doctor;

    private User patientUser;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setId(1L);
        doctor.setEmail(DOCTOR_EMAIL);
        doctor.setRole(UserRole.DOCTOR);

        patientUser = new User();
        patientUser.setId(3L);
        patientUser.setFullName("Jane Patient");
        patientUser.setRole(UserRole.PATIENT);

        patient = new PatientProfile();
        patient.setId(2L);
        patient.setUser(patientUser);
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("grantDoctorAccess: links the doctor to the current patient's card")
    void grantDoctorAccess_success() {
        CareLinkResponseDto responseDto = new CareLinkResponseDto(
                100L, doctor.getId(), "Dr. House", patient.getId(), "Jane Patient",
                LocalDateTime.now());

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(doctor));
        when(careLinkRepository.existsByDoctorIdAndPatientId(doctor.getId(), patient.getId()))
                .thenReturn(false);
        when(careLinkRepository.save(any(CareLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(careLinkMapper.toDto(any(CareLink.class))).thenReturn(responseDto);

        CareLinkResponseDto result = careLinkService.grantDoctorAccess(
                patientUser, new CareLinkRequestDto(DOCTOR_EMAIL));

        ArgumentCaptor<CareLink> captor = ArgumentCaptor.forClass(CareLink.class);
        verify(careLinkRepository).save(captor.capture());
        assertThat(captor.getValue().getDoctor()).isEqualTo(doctor);
        assertThat(captor.getValue().getPatient()).isEqualTo(patient);
        assertThat(result).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("grantDoctorAccess: rejects an email that belongs to a non-doctor")
    void grantDoctorAccess_userIsNotDoctor() {
        User otherPatient = new User();
        otherPatient.setId(7L);
        otherPatient.setRole(UserRole.PATIENT);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherPatient));

        assertThrows(UserNotFoundException.class, () -> careLinkService.grantDoctorAccess(
                patientUser, new CareLinkRequestDto("other@test.com")));

        verify(careLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("grantDoctorAccess: rejects an unknown email")
    void grantDoctorAccess_doctorNotFound() {
        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> careLinkService.grantDoctorAccess(
                patientUser, new CareLinkRequestDto("nobody@test.com")));
    }

    @Test
    @DisplayName("grantDoctorAccess: rejects granting the same doctor twice")
    void grantDoctorAccess_duplicate() {
        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(userRepository.findByEmail(DOCTOR_EMAIL)).thenReturn(Optional.of(doctor));
        when(careLinkRepository.existsByDoctorIdAndPatientId(doctor.getId(), patient.getId()))
                .thenReturn(true);

        assertThrows(CareLinkAlreadyExistsException.class, () -> careLinkService
                .grantDoctorAccess(patientUser, new CareLinkRequestDto(DOCTOR_EMAIL)));

        verify(careLinkRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMyDoctors: returns the current patient's care links")
    void getMyDoctors_success() {
        CareLink careLink = new CareLink();
        CareLinkResponseDto responseDto = new CareLinkResponseDto(
                100L, doctor.getId(), "Dr. House", patient.getId(), "Jane Patient",
                LocalDateTime.now());

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(careLinkRepository.findByPatientId(patient.getId())).thenReturn(List.of(careLink));
        when(careLinkMapper.toDto(careLink)).thenReturn(responseDto);

        assertThat(careLinkService.getMyDoctors(patientUser)).containsExactly(responseDto);
    }

    @Test
    @DisplayName("revokeDoctorAccess: deletes the patient's own care link")
    void revokeDoctorAccess_success() {
        CareLink careLink = new CareLink();
        careLink.setId(100L);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(careLinkRepository.findByIdAndPatientId(100L, patient.getId()))
                .thenReturn(Optional.of(careLink));

        careLinkService.revokeDoctorAccess(patientUser, 100L);

        verify(careLinkRepository).delete(careLink);
    }

    @Test
    @DisplayName("revokeDoctorAccess: another patient's care link is not found")
    void revokeDoctorAccess_careLinkOfAnotherPatient() {
        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(careLinkRepository.findByIdAndPatientId(100L, patient.getId()))
                .thenReturn(Optional.empty());

        assertThrows(CareLinkNotFoundException.class,
                () -> careLinkService.revokeDoctorAccess(patientUser, 100L));

        verify(careLinkRepository, never()).delete(any(CareLink.class));
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
