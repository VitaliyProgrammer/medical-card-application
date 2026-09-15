package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.ReminderRequestDto;
import com.vitaliy.medcard.dto.ReminderResponseDto;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.PatientProfileNotFoundException;
import com.vitaliy.medcard.exception.ReminderNotFoundException;
import com.vitaliy.medcard.mapper.ReminderMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.Reminder;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.ReminderRepository;
import com.vitaliy.medcard.service.ReminderService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReminderServiceImpl implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final ReminderMapper reminderMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ReminderResponseDto> getMyReminders(User currentUser) {
        PatientProfile patient = findPatientByUserId(currentUser.getId());

        return reminderRepository.findByPatientId(patient.getId()).stream()
                .map(reminderMapper::toDto)
                .toList();
    }

    @Override
    @Audited(action = "ADD_REMINDER")
    public ReminderResponseDto addReminder(User currentUser, ReminderRequestDto request) {
        PatientProfile patient = findPatientByUserId(currentUser.getId());

        Reminder reminder = reminderMapper.toEntity(request);
        reminder.setPatient(patient);
        reminder.setNotified(false);
        reminder.setCreatedAt(LocalDateTime.now());

        return reminderMapper.toDto(reminderRepository.save(reminder));
    }

    @Override
    @Audited(action = "DELETE_REMINDER")
    public void deleteMyReminder(User currentUser, Long reminderId) {
        Reminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new ReminderNotFoundException(
                        "Reminder not found: " + reminderId + "!"));

        if (!reminder.getPatient().getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenActionException("This reminder doesn't belong to you!");
        }

        reminderRepository.delete(reminder);
    }

    private PatientProfile findPatientByUserId(Long userId) {
        return patientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new PatientProfileNotFoundException(
                        "Patient profile not found for user: " + userId + "!"));
    }
}
