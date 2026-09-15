package com.vitaliy.medcard.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.Reminder;
import com.vitaliy.medcard.repository.ReminderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReminderScanSchedulerTest {

    @Mock
    private ReminderRepository reminderRepository;

    @InjectMocks
    private ReminderScanScheduler reminderScanScheduler;

    @Test
    @DisplayName("scanOverdueReminders: marks overdue reminders as notified and saves them")
    void scanOverdueReminders_marksAndSaves() {
        PatientProfile patient = new PatientProfile();
        patient.setId(10L);

        Reminder overdue = new Reminder();
        overdue.setId(1L);
        overdue.setPatient(patient);
        overdue.setDueAt(LocalDateTime.now().minusMinutes(10));
        overdue.setNotified(false);

        when(reminderRepository.findByDueAtBeforeAndNotifiedFalse(any()))
                .thenReturn(List.of(overdue));

        reminderScanScheduler.scanOverdueReminders();

        ArgumentCaptor<List<Reminder>> captor = ArgumentCaptor.forClass(List.class);
        verify(reminderRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).containsExactly(overdue);
        assertThat(overdue.isNotified()).isTrue();
    }

    @Test
    @DisplayName("scanOverdueReminders: does nothing when there are no overdue reminders")
    void scanOverdueReminders_noneOverdue() {
        when(reminderRepository.findByDueAtBeforeAndNotifiedFalse(any()))
                .thenReturn(List.of());

        reminderScanScheduler.scanOverdueReminders();

        verify(reminderRepository, never()).saveAll(anyList());
    }
}
