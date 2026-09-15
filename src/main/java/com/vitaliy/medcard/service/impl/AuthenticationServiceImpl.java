package com.vitaliy.medcard.service.impl;

import com.vitaliy.medcard.aspect.Audited;
import com.vitaliy.medcard.dto.RefreshTokenRequestDto;
import com.vitaliy.medcard.dto.UserLoginRequestDto;
import com.vitaliy.medcard.dto.UserLoginResponseDto;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.dto.UserRegistrationResponseDto;
import com.vitaliy.medcard.exception.InvalidCredentialsException;
import com.vitaliy.medcard.exception.InvalidRefreshTokenException;
import com.vitaliy.medcard.exception.RegistrationException;
import com.vitaliy.medcard.mapper.UserMapper;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.RefreshToken;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.RefreshTokenRepository;
import com.vitaliy.medcard.repository.UserRepository;
import com.vitaliy.medcard.security.JwtUtil;
import com.vitaliy.medcard.service.AuthenticationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final long refreshTokenExpirationMs;

    public AuthenticationServiceImpl(
            UserRepository userRepository,
            PatientProfileRepository patientProfileRepository,
            RefreshTokenRepository refreshTokenRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

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
    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password!"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password!");
        }

        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public UserLoginResponseDto refresh(RefreshTokenRequestDto request) {

        RefreshToken storedToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid!"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token is expired or revoked!");
        }

        // Rotation: burn the old refresh token so a stolen copy can't be replayed
        // after the legitimate client has already used it once.
        storedToken.setRevoked(true);

        return issueTokenPair(storedToken.getUser());
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequestDto request) {
        refreshTokenRepository.findByToken(request.refreshToken())
                .ifPresent(token -> token.setRevoked(true));
    }

    private UserLoginResponseDto issueTokenPair(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(
                LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs)));
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        return new UserLoginResponseDto(accessToken, refreshToken.getToken());
    }
}
