package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.CareLinkRequestDto;
import com.vitaliy.medcard.dto.CareLinkResponseDto;
import com.vitaliy.medcard.dto.PatientSummaryDto;
import com.vitaliy.medcard.model.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CareLinkService {

    CareLinkResponseDto grantDoctorAccess(User currentPatient, CareLinkRequestDto request);

    List<CareLinkResponseDto> getMyDoctors(User currentPatient);

    void revokeDoctorAccess(User currentPatient, Long careLinkId);

    Page<PatientSummaryDto> searchMyPatients(User currentDoctor, String search, Pageable pageable);
}
