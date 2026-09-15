package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.UserLoginRequestDto;
import com.vitaliy.medcard.dto.UserLoginResponseDto;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.dto.UserRegistrationResponseDto;

public interface AuthenticationService {

    UserRegistrationResponseDto register(UserRegistrationRequestDto request);

    UserLoginResponseDto login(UserLoginRequestDto request);
}
