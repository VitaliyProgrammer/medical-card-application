package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.ConditionRequestDto;
import com.vitaliy.medcard.dto.ConditionResponseDto;
import com.vitaliy.medcard.exception.ConditionNotFoundException;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.mapper.ConditionMapper;
import com.vitaliy.medcard.model.Condition;
import com.vitaliy.medcard.model.status.ConditionStatus;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.ConditionRepository;
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
class ConditionServiceImplTest {

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private ConditionMapper conditionMapper;

    @Mock
    private PatientAccessValidator patientAccessValidator;

    @InjectMocks
    private ConditionServiceImpl conditionService;

    private User patientUser;

    private PatientProfile patient;

    private Condition condition;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(1L);

        patient = new PatientProfile();
        patient.setId(10L);
        patient.setUser(patientUser);

        condition = new Condition();
        condition.setId(200L);
        condition.setPatient(patient);
        condition.setName("Asthma");
        condition.setStatus(ConditionStatus.ACTIVE);
    }

    @Test
    @DisplayName("addCondition: attaches the new condition to the caller's own card")
    void addCondition_success() {
        ConditionRequestDto request =
                new ConditionRequestDto("Asthma", ConditionStatus.ACTIVE, null);
        ConditionResponseDto responseDto =
                new ConditionResponseDto(200L, "Asthma", ConditionStatus.ACTIVE, null, null);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(conditionMapper.toEntity(request)).thenReturn(condition);
        when(conditionRepository.save(condition)).thenReturn(condition);
        when(conditionMapper.toDto(condition)).thenReturn(responseDto);

        ConditionResponseDto result = conditionService.addCondition(patientUser, request);

        assertThat(result).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("deleteMyCondition: removes a condition that belongs to the caller")
    void deleteMyCondition_ownCondition() {
        when(conditionRepository.findById(condition.getId())).thenReturn(Optional.of(condition));

        conditionService.deleteMyCondition(patientUser, condition.getId());

        verify(conditionRepository).delete(condition);
    }

    @Test
    @DisplayName("deleteMyCondition: rejects deleting someone else's condition")
    void deleteMyCondition_notOwner() {
        User anotherPatient = new User();
        anotherPatient.setId(2L);

        when(conditionRepository.findById(condition.getId())).thenReturn(Optional.of(condition));

        assertThrows(ForbiddenActionException.class,
                () -> conditionService.deleteMyCondition(anotherPatient, condition.getId()));

        verify(conditionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteMyCondition: throws when the condition doesn't exist")
    void deleteMyCondition_notFound() {
        when(conditionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ConditionNotFoundException.class,
                () -> conditionService.deleteMyCondition(patientUser, 999L));
    }

    @Test
    @DisplayName("getConditionsForDoctor: returns the list once access is validated")
    void getConditionsForDoctor_success() {
        User doctor = new User();
        doctor.setId(5L);

        ConditionResponseDto responseDto =
                new ConditionResponseDto(200L, "Asthma", ConditionStatus.ACTIVE, null, null);

        doNothing().when(patientAccessValidator).validateDoctorAccess(doctor, patient.getId());
        when(conditionRepository.findByPatientId(patient.getId())).thenReturn(List.of(condition));
        when(conditionMapper.toDto(condition)).thenReturn(responseDto);

        List<ConditionResponseDto> result =
                conditionService.getConditionsForDoctor(doctor, patient.getId());

        assertThat(result).containsExactly(responseDto);
    }
}
