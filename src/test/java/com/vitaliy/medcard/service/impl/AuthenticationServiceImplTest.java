package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.dto.RefreshTokenRequestDto;
import com.vitaliy.medcard.dto.UserLoginRequestDto;
import com.vitaliy.medcard.dto.UserLoginResponseDto;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.dto.UserRegistrationResponseDto;
import com.vitaliy.medcard.exception.InvalidCredentialsException;
import com.vitaliy.medcard.exception.InvalidRefreshTokenException;
import com.vitaliy.medcard.exception.RegistrationException;
import com.vitaliy.medcard.mapper.UserMapper;
import com.vitaliy.medcard.model.RefreshToken;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.RefreshTokenRepository;
import com.vitaliy.medcard.repository.UserRepository;
import com.vitaliy.medcard.security.JwtUtil;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceImplTest {

    private static final long REFRESH_TOKEN_EXPIRATION_MS = 2_592_000_000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    private AuthenticationServiceImpl authenticationService;

    private UserRegistrationRequestDto patientRequest;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(
                userRepository, patientProfileRepository, refreshTokenRepository,
                userMapper, passwordEncoder, jwtUtil, REFRESH_TOKEN_EXPIRATION_MS);

        patientRequest = new UserRegistrationRequestDto();
        patientRequest.setEmail("patient@test.com");
        patientRequest.setPassword("password123");
        patientRequest.setRepeatPassword("password123");
        patientRequest.setFullName("Jane Patient");
        patientRequest.setRole(UserRole.PATIENT);
    }

    @Test
    @DisplayName("register: creates a User and a PatientProfile for a PATIENT registration")
    void register_patient_createsProfile() {
        when(userRepository.existsByEmail(patientRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(patientRequest.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenReturn(
                new UserRegistrationResponseDto(1L, patientRequest.getEmail(),
                        patientRequest.getFullName(), UserRole.PATIENT));

        UserRegistrationResponseDto result = authenticationService.register(patientRequest);

        assertThat(result.email()).isEqualTo(patientRequest.getEmail());
        verify(patientProfileRepository).save(any());
    }

    @Test
    @DisplayName("register: does not create a PatientProfile for a DOCTOR registration")
    void register_doctor_doesNotCreateProfile() {
        patientRequest.setRole(UserRole.DOCTOR);

        when(userRepository.existsByEmail(patientRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(patientRequest.getPassword())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toDto(any(User.class))).thenReturn(
                new UserRegistrationResponseDto(1L, patientRequest.getEmail(),
                        patientRequest.getFullName(), UserRole.DOCTOR));

        authenticationService.register(patientRequest);

        verify(patientProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: rejects self-registration as ADMIN")
    void register_admin_isRejected() {
        patientRequest.setRole(UserRole.ADMIN);

        assertThrows(RegistrationException.class,
                () -> authenticationService.register(patientRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: rejects a duplicate email")
    void register_duplicateEmail_isRejected() {
        when(userRepository.existsByEmail(patientRequest.getEmail())).thenReturn(true);

        assertThrows(RegistrationException.class,
                () -> authenticationService.register(patientRequest));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: returns an access token and a refresh token for correct credentials")
    void login_success() {
        User user = new User();
        user.setEmail("patient@test.com");
        user.setPassword("hashed");
        user.setRole(UserRole.PATIENT);

        UserLoginRequestDto request = new UserLoginRequestDto("patient@test.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name())).thenReturn("jwt-token");

        UserLoginResponseDto result = authenticationService.login(request);

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.refreshToken()).isNotBlank();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login: rejects an unknown email")
    void login_unknownEmail_isRejected() {
        UserLoginRequestDto request = new UserLoginRequestDto("nobody@test.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.login(request));
    }

    @Test
    @DisplayName("login: rejects a wrong password")
    void login_wrongPassword_isRejected() {
        User user = new User();
        user.setEmail("patient@test.com");
        user.setPassword("hashed");

        UserLoginRequestDto request = new UserLoginRequestDto("patient@test.com", "wrong-password");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authenticationService.login(request));
    }

    @Test
    @DisplayName("refresh: rotates the refresh token and issues a new pair")
    void refresh_success_rotatesToken() {
        User user = new User();
        user.setEmail("patient@test.com");
        user.setRole(UserRole.PATIENT);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken("old-refresh-token");
        storedToken.setUser(user);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        storedToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("old-refresh-token"))
                .thenReturn(Optional.of(storedToken));
        when(jwtUtil.generateToken(user.getEmail(), user.getRole().name()))
                .thenReturn("new-jwt-token");

        UserLoginResponseDto result =
                authenticationService.refresh(new RefreshTokenRequestDto("old-refresh-token"));

        assertThat(result.token()).isEqualTo("new-jwt-token");
        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refresh: rejects an unknown token")
    void refresh_unknownToken_isRejected() {
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class,
                () -> authenticationService.refresh(new RefreshTokenRequestDto("nonexistent")));
    }

    @Test
    @DisplayName("refresh: rejects an expired token")
    void refresh_expiredToken_isRejected() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken("expired-token");
        storedToken.setUser(new User());
        storedToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        storedToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(storedToken));

        assertThrows(InvalidRefreshTokenException.class,
                () -> authenticationService.refresh(new RefreshTokenRequestDto("expired-token")));
    }

    @Test
    @DisplayName("refresh: rejects an already-revoked token")
    void refresh_revokedToken_isRejected() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken("revoked-token");
        storedToken.setUser(new User());
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        storedToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(storedToken));

        assertThrows(InvalidRefreshTokenException.class,
                () -> authenticationService.refresh(new RefreshTokenRequestDto("revoked-token")));
    }

    @Test
    @DisplayName("logout: revokes an existing refresh token")
    void logout_revokesToken() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken("some-token");
        storedToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("some-token")).thenReturn(Optional.of(storedToken));

        authenticationService.logout(new RefreshTokenRequestDto("some-token"));

        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("logout: is a no-op for an unknown token")
    void logout_unknownToken_isNoOp() {
        when(refreshTokenRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        authenticationService.logout(new RefreshTokenRequestDto("nonexistent"));

        verify(refreshTokenRepository, never()).save(any());
    }
}
