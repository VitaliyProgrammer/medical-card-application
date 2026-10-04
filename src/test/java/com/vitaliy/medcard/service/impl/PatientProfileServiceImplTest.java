package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.PatientProfileRequestDto;
import com.vitaliy.medcard.dto.PatientProfileResponseDto;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.mapper.PatientProfileMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.validation.PatientAccessValidator;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceImplTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private PatientProfileMapper patientProfileMapper;

    @Mock
    private PatientAccessValidator patientAccessValidator;

    @InjectMocks
    private PatientProfileServiceImpl patientProfileService;

    private User patientUser;

    private PatientProfile profile;

    private PatientProfileResponseDto responseDto;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(1L);
        patientUser.setRole(UserRole.PATIENT);

        profile = new PatientProfile();
        profile.setId(10L);
        profile.setUser(patientUser);

        responseDto = new PatientProfileResponseDto(
                10L, "Jane Patient", "patient@test.com", null, null, null, null, null, null);
    }

    @Test
    @DisplayName("getMyProfile: returns the caller's own card")
    void getMyProfile_success() {
        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(profile));
        when(patientProfileMapper.toDto(profile)).thenReturn(responseDto);

        PatientProfileResponseDto result = patientProfileService.getMyProfile(patientUser);

        assertThat(result).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("getMyProfile: throws when the caller has no card yet")
    void getMyProfile_notFound() {
        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.empty());

        assertThrows(PatientProfileNotFoundException.class,
                () -> patientProfileService.getMyProfile(patientUser));
    }

    @Test
    @DisplayName("updateMyProfile: applies changes from the request onto the entity")
    void updateMyProfile_success() {
        PatientProfileRequestDto request = new PatientProfileRequestDto();
        request.setBloodGroup("O+");

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(profile));
        when(patientProfileMapper.toDto(profile)).thenReturn(responseDto);

        PatientProfileResponseDto result =
                patientProfileService.updateMyProfile(patientUser, request);

        assertThat(result).isEqualTo(responseDto);
        assertThat(profile.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getProfileForDoctor: returns the card when the doctor is assigned")
    void getProfileForDoctor_assignedDoctor() {
        User doctor = new User();
        doctor.setId(2L);
        doctor.setRole(UserRole.DOCTOR);

        when(patientProfileRepository.findById(profile.getId()))
                .thenReturn(Optional.of(profile));
        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, profile.getId());
        when(patientProfileMapper.toDto(profile)).thenReturn(responseDto);

        PatientProfileResponseDto result =
                patientProfileService.getProfileForDoctor(doctor, profile.getId());

        assertThat(result).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("getProfileForDoctor: propagates the forbidden error for an unassigned doctor")
    void getProfileForDoctor_notAssigned() {
        User doctor = new User();
        doctor.setId(2L);
        doctor.setRole(UserRole.DOCTOR);

        when(patientProfileRepository.findById(profile.getId()))
                .thenReturn(Optional.of(profile));
        doThrow(new ForbiddenActionException("You are not assigned to this patient's care!"))
                .when(patientAccessValidator).validateDoctorAccess(doctor, profile.getId());

        assertThrows(ForbiddenActionException.class,
                () -> patientProfileService.getProfileForDoctor(doctor, profile.getId()));
    }

    @Test
    @DisplayName("getProfileForDoctor: throws when the patient profile doesn't exist")
    void getProfileForDoctor_patientNotFound() {
        User doctor = new User();
        doctor.setId(2L);

        when(patientProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(PatientProfileNotFoundException.class,
                () -> patientProfileService.getProfileForDoctor(doctor, 999L));
    }
}
