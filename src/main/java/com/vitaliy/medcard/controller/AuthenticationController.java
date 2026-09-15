package com.vitaliy.medcard.controller;

import com.vitaliy.medcard.dto.UserLoginRequestDto;
import com.vitaliy.medcard.dto.UserLoginResponseDto;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.dto.UserRegistrationResponseDto;
import com.vitaliy.medcard.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration and login for patients and doctors")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new patient or doctor account")
    public UserRegistrationResponseDto registration(
            @RequestBody @Valid UserRegistrationRequestDto request) {
        return authenticationService.register(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Log in and receive a JWT access token")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto request) {
        return authenticationService.login(request);
    }
}
