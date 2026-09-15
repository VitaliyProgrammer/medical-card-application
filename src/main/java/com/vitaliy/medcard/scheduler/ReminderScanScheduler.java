package com.vitaliy.medcard.scheduler;

import com.vitaliy.medcard.model.Reminder;
import com.vitaliy.medcard.repository.ReminderRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReminderScanScheduler {

    private final ReminderRepository reminderRepository;

    @Scheduled(cron = "${app.reminders.scan-cron:0 */5 * * * *}")
    @Transactional
    public void scanOverdueReminders() {
        List<Reminder> overdueReminders =
                reminderRepository.findByDueAtBeforeAndNotifiedFalse(LocalDateTime.now());

        for (Reminder reminder : overdueReminders) {
            log.info("Reminder due for patient {}: [{}] {}",
                    reminder.getPatient().getId(), reminder.getType(), reminder.getTitle());
            reminder.setNotified(true);
        }

        if (!overdueReminders.isEmpty()) {
            reminderRepository.saveAll(overdueReminders);
        }
    }
}
