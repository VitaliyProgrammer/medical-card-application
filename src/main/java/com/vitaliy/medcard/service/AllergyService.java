package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.AllergyRequestDto;
import com.vitaliy.medcard.dto.AllergyResponseDto;
import com.vitaliy.medcard.model.User;
import java.util.List;

public interface AllergyService {

    List<AllergyResponseDto> getMyAllergies(User currentUser);

    AllergyResponseDto addAllergy(User currentUser, AllergyRequestDto request);

    void deleteMyAllergy(User currentUser, Long allergyId);

    List<AllergyResponseDto> getAllergiesForDoctor(User currentDoctor, Long patientId);
}
