package com.vitaliy.medcard.repository;

import com.vitaliy.medcard.model.Reminder;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    List<Reminder> findByPatientId(Long patientId);

    List<Reminder> findByDueAtBeforeAndNotifiedFalse(LocalDateTime now);
}
