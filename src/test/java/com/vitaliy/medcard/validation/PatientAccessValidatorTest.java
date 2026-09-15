package com.vitaliy.medcard.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.CareLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientAccessValidatorTest {

    @Mock
    private CareLinkRepository careLinkRepository;

    @InjectMocks
    private PatientAccessValidator patientAccessValidator;

    @Test
    @DisplayName("validateDoctorAccess: allows an assigned doctor")
    void validateDoctorAccess_assignedDoctor() {
        User doctor = new User();
        doctor.setId(1L);
        doctor.setRole(UserRole.DOCTOR);

        when(careLinkRepository.existsByDoctorIdAndPatientId(1L, 10L)).thenReturn(true);

        assertDoesNotThrow(() -> patientAccessValidator.validateDoctorAccess(doctor, 10L));
    }

    @Test
    @DisplayName("validateDoctorAccess: always allows ADMIN without querying CareLink")
    void validateDoctorAccess_admin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(UserRole.ADMIN);

        assertDoesNotThrow(() -> patientAccessValidator.validateDoctorAccess(admin, 10L));
        verifyNoInteractions(careLinkRepository);
    }

    @Test
    @DisplayName("validateDoctorAccess: rejects an unassigned doctor")
    void validateDoctorAccess_unassignedDoctor() {
        User doctor = new User();
        doctor.setId(1L);
        doctor.setRole(UserRole.DOCTOR);

        when(careLinkRepository.existsByDoctorIdAndPatientId(1L, 10L)).thenReturn(false);

        assertThrows(ForbiddenActionException.class,
                () -> patientAccessValidator.validateDoctorAccess(doctor, 10L));
    }
}
