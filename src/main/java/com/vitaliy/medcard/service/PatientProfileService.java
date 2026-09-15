package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.PatientProfileRequestDto;
import com.vitaliy.medcard.dto.PatientProfileResponseDto;
import com.vitaliy.medcard.model.User;

public interface PatientProfileService {

    PatientProfileResponseDto getMyProfile(User currentUser);

    PatientProfileResponseDto updateMyProfile(User currentUser, PatientProfileRequestDto request);

    PatientProfileResponseDto getProfileForDoctor(User currentDoctor, Long patientId);
}
