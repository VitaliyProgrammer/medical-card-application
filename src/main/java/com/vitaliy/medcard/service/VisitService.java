package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.VisitRequestDto;
import com.vitaliy.medcard.dto.VisitResponseDto;
import com.vitaliy.medcard.model.User;
import java.util.List;

public interface VisitService {

    VisitResponseDto createVisit(User currentDoctor, Long patientId, VisitRequestDto request);

    List<VisitResponseDto> getVisitsForDoctor(User currentDoctor, Long patientId);

    List<VisitResponseDto> getMyVisits(User currentUser);
}
