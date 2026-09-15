package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.ConditionRequestDto;
import com.vitaliy.medcard.dto.ConditionResponseDto;
import com.vitaliy.medcard.model.User;
import java.util.List;

public interface ConditionService {

    List<ConditionResponseDto> getMyConditions(User currentUser);

    ConditionResponseDto addCondition(User currentUser, ConditionRequestDto request);

    void deleteMyCondition(User currentUser, Long conditionId);

    List<ConditionResponseDto> getConditionsForDoctor(User currentDoctor, Long patientId);
}
