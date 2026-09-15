package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.ReminderRequestDto;
import com.vitaliy.medcard.dto.ReminderResponseDto;
import com.vitaliy.medcard.exception.ForbiddenActionException;
import com.vitaliy.medcard.exception.ReminderNotFoundException;
import com.vitaliy.medcard.mapper.ReminderMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.Reminder;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.ReminderType;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.ReminderRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderServiceImplTest {

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private ReminderMapper reminderMapper;

    @InjectMocks
    private ReminderServiceImpl reminderService;

    private User patientUser;

    private PatientProfile patient;

    private Reminder reminder;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setId(1L);

        patient = new PatientProfile();
        patient.setId(10L);
        patient.setUser(patientUser);

        reminder = new Reminder();
        reminder.setId(100L);
        reminder.setPatient(patient);
        reminder.setTitle("Take medication");
        reminder.setType(ReminderType.MEDICATION);
        reminder.setDueAt(LocalDateTime.now().plusHours(1));
    }

    @Test
    @DisplayName("addReminder: attaches the new reminder to the caller's own card")
    void addReminder_success() {
        ReminderRequestDto request = new ReminderRequestDto(
                "Take medication", ReminderType.MEDICATION, LocalDateTime.now().plusHours(1));
        ReminderResponseDto responseDto = new ReminderResponseDto(
                100L, "Take medication", ReminderType.MEDICATION, request.dueAt(), false);

        when(patientProfileRepository.findByUserId(patientUser.getId()))
                .thenReturn(Optional.of(patient));
        when(reminderMapper.toEntity(request)).thenReturn(reminder);
        when(reminderRepository.save(reminder)).thenReturn(reminder);
        when(reminderMapper.toDto(reminder)).thenReturn(responseDto);

        ReminderResponseDto result = reminderService.addReminder(patientUser, request);

        assertThat(result).isEqualTo(responseDto);
        assertThat(reminder.isNotified()).isFalse();
    }

    @Test
    @DisplayName("deleteMyReminder: removes a reminder that belongs to the caller")
    void deleteMyReminder_ownReminder() {
        when(reminderRepository.findById(reminder.getId())).thenReturn(Optional.of(reminder));

        reminderService.deleteMyReminder(patientUser, reminder.getId());

        verify(reminderRepository).delete(reminder);
    }

    @Test
    @DisplayName("deleteMyReminder: rejects deleting someone else's reminder")
    void deleteMyReminder_notOwner() {
        User anotherPatient = new User();
        anotherPatient.setId(2L);

        when(reminderRepository.findById(reminder.getId())).thenReturn(Optional.of(reminder));

        assertThrows(ForbiddenActionException.class,
                () -> reminderService.deleteMyReminder(anotherPatient, reminder.getId()));

        verify(reminderRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteMyReminder: throws when the reminder doesn't exist")
    void deleteMyReminder_notFound() {
        when(reminderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ReminderNotFoundException.class,
                () -> reminderService.deleteMyReminder(patientUser, 999L));
    }
}
