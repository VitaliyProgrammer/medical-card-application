package com.vitaliy.medcard.service;

import com.vitaliy.medcard.dto.ForgotPasswordRequestDto;
import com.vitaliy.medcard.dto.RefreshTokenRequestDto;
import com.vitaliy.medcard.dto.ResetPasswordRequestDto;
import com.vitaliy.medcard.dto.UserLoginRequestDto;
import com.vitaliy.medcard.dto.UserLoginResponseDto;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.dto.UserRegistrationResponseDto;

public interface AuthenticationService {

    UserRegistrationResponseDto register(UserRegistrationRequestDto request);

    UserLoginResponseDto login(UserLoginRequestDto request);

    UserLoginResponseDto refresh(RefreshTokenRequestDto request);

    void logout(RefreshTokenRequestDto request);

    void forgotPassword(ForgotPasswordRequestDto request);

    void resetPassword(ResetPasswordRequestDto request);
}
