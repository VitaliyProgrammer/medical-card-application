package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.AllergyRequestDto;
import com.vitaliy.medcard.dto.AllergyResponseDto;
import com.vitaliy.medcard.exception.AllergyNotFoundException;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.mapper.AllergyMapper;
import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.status.AllergySeverity;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.AllergyRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
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
class AllergyServiceImplTest {

    @Mock
    private AllergyRepository allergyRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private AllergyMapper allergyMapper;

    @Mock
    private PatientAccessValidator patientAccessValidator;

    @InjectMocks
    private AllergyServiceImpl allergyService;

    private User patientUser;

    private PatientProfile patient;

    private Allergy allergy;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(1L);

        patient = new PatientProfile();
        patient.setId(10L);
        patient.setUser(patientUser);

        allergy = new Allergy();
        allergy.setId(100L);
        allergy.setPatient(patient);
        allergy.setName("Penicillin");
        allergy.setSeverity(AllergySeverity.SEVERE);
    }

    @Test
    @DisplayName("addAllergy: attaches the new allergy to the caller's own card")
    void addAllergy_success() {
        AllergyRequestDto request = new AllergyRequestDto("Penicillin", AllergySeverity.SEVERE);
        AllergyResponseDto responseDto =
                new AllergyResponseDto(100L, "Penicillin", AllergySeverity.SEVERE, null);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(allergyMapper.toEntity(request)).thenReturn(allergy);
        when(allergyRepository.save(allergy)).thenReturn(allergy);
        when(allergyMapper.toDto(allergy)).thenReturn(responseDto);

        AllergyResponseDto result = allergyService.addAllergy(patientUser, request);

        assertThat(result).isEqualTo(responseDto);
        assertThat(allergy.getPatient()).isEqualTo(patient);
    }

    @Test
    @DisplayName("deleteMyAllergy: removes an allergy that belongs to the caller")
    void deleteMyAllergy_ownAllergy() {
        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));

        allergyService.deleteMyAllergy(patientUser, allergy.getId());

        verify(allergyRepository).delete(allergy);
    }

    @Test
    @DisplayName("deleteMyAllergy: rejects deleting someone else's allergy")
    void deleteMyAllergy_notOwner() {
        User anotherPatient = new User();
        anotherPatient.setId(2L);

        when(allergyRepository.findById(allergy.getId())).thenReturn(Optional.of(allergy));

        assertThrows(ForbiddenActionException.class,
                () -> allergyService.deleteMyAllergy(anotherPatient, allergy.getId()));

        verify(allergyRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteMyAllergy: throws when the allergy doesn't exist")
    void deleteMyAllergy_notFound() {
        when(allergyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AllergyNotFoundException.class,
                () -> allergyService.deleteMyAllergy(patientUser, 999L));
    }

    @Test
    @DisplayName("getAllergiesForDoctor: returns the list once access is validated")
    void getAllergiesForDoctor_success() {
        User doctor = new User();
        doctor.setId(5L);

        AllergyResponseDto responseDto =
                new AllergyResponseDto(100L, "Penicillin", AllergySeverity.SEVERE, null);

        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());
        when(allergyRepository.findByPatientId(patient.getId())).thenReturn(List.of(allergy));
        when(allergyMapper.toDto(allergy)).thenReturn(responseDto);

        List<AllergyResponseDto> result =
                allergyService.getAllergiesForDoctor(doctor, patient.getId());

        assertThat(result).containsExactly(responseDto);
    }
}
