package com.vitaliy.medcard.validation;

import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.CareLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatientAccessValidator {

    private final CareLinkRepository careLinkRepository;

    public void validateDoctorAccess(User currentDoctor, Long patientId) {
        if (currentDoctor.getRole() == UserRole.ADMIN) {
            return;
        }

        boolean isAssignedDoctor = careLinkRepository.existsByDoctorIdAndPatientId(
                currentDoctor.getId(), patientId);

        if (!isAssignedDoctor) {
            throw new ForbiddenActionException("You are not assigned to this patient's care!");
        }
    }
}
