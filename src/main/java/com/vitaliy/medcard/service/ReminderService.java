package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.ReminderRequestDto;
import com.vitaliy.medcard.dto.ReminderResponseDto;
import com.vitaliy.medcard.model.User;
import java.util.List;

public interface ReminderService {

    List<ReminderResponseDto> getMyReminders(User currentUser);

    ReminderResponseDto addReminder(User currentUser, ReminderRequestDto request);

    void deleteMyReminder(User currentUser, Long reminderId);
}
