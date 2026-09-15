package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.UserLoginRequestDto;
import com.vitaliy.medcard.dto.UserLoginResponseDto;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.dto.UserRegistrationResponseDto;
import com.vitaliy.medcard.exception.InvalidCredentialsException;
import com.vitaliy.medcard.exception.RegistrationException;
import com.vitaliy.medcard.mapper.UserMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.UserRepository;
import com.vitaliy.medcard.security.JwtUtil;
import com.vitaliy.medcard.service.AuthenticationService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    @Audited(action = "REGISTER_USER")
    public UserRegistrationResponseDto register(UserRegistrationRequestDto request) {

        if (request.getRole() == UserRole.ADMIN) {
            throw new RegistrationException("Self-registration as ADMIN is not allowed!");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RegistrationException(
                    "User with email " + request.getEmail() + " already exists!");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setCreatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        if (user.getRole() == UserRole.PATIENT) {
            PatientProfile profile = new PatientProfile();
            profile.setUser(user);
            profile.setCreatedAt(LocalDateTime.now());
            profile.setUpdatedAt(LocalDateTime.now());
            patientProfileRepository.save(profile);
        }

        return userMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserLoginResponseDto login(UserLoginRequestDto request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password!"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password!");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return new UserLoginResponseDto(token);
    }
}
