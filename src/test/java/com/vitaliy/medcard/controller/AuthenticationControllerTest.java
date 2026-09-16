package com.vitaliy.medcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.dto.RefreshTokenRequestDto;
import com.vitaliy.medcard.dto.UserLoginRequestDto;
import com.vitaliy.medcard.dto.UserLoginResponseDto;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.dto.UserRegistrationResponseDto;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class AuthenticationControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setEmail("patient@test.com");
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setFullName("Jane Patient");
        existingUser.setRole(UserRole.PATIENT);
        existingUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(existingUser);
    }

    @Test
    @DisplayName("POST /api/auth/registration: creates a new account")
    void registration_success() throws Exception {
        UserRegistrationRequestDto request = new UserRegistrationRequestDto();
        request.setEmail("new.doctor@test.com");
        request.setPassword("password123");
        request.setRepeatPassword("password123");
        request.setFullName("Dr. House");
        request.setRole(UserRole.DOCTOR);

        MvcResult result = mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UserRegistrationResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserRegistrationResponseDto.class);

        assertThat(response.email()).isEqualTo("new.doctor@test.com");
        assertThat(response.role()).isEqualTo(UserRole.DOCTOR);
    }

    @Test
    @DisplayName("POST /api/auth/registration: rejects a duplicate email")
    void registration_duplicateEmail() throws Exception {
        UserRegistrationRequestDto request = new UserRegistrationRequestDto();
        request.setEmail(existingUser.getEmail());
        request.setPassword("password123");
        request.setRepeatPassword("password123");
        request.setFullName("Another Jane");
        request.setRole(UserRole.PATIENT);

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/auth/registration: rejects mismatched passwords")
    void registration_passwordMismatch() throws Exception {
        UserRegistrationRequestDto request = new UserRegistrationRequestDto();
        request.setEmail("mismatch@test.com");
        request.setPassword("password123");
        request.setRepeatPassword("different456");
        request.setFullName("Someone");
        request.setRole(UserRole.PATIENT);

        mockMvc.perform(post("/api/auth/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login: returns a token for correct credentials")
    void login_success() throws Exception {
        UserLoginRequestDto request = new UserLoginRequestDto("patient@test.com", "password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        UserLoginResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), UserLoginResponseDto.class);

        assertThat(response.token()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/auth/login: rejects a wrong password")
    void login_wrongPassword() throws Exception {
        UserLoginRequestDto request = new UserLoginRequestDto("patient@test.com", "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/refresh: issues a new pair and rotates the old refresh token")
    void refresh_success() throws Exception {
        UserLoginResponseDto loginResponse = loginAsExistingUser();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequestDto(loginResponse.refreshToken()))))
                .andExpect(status().isOk())
                .andReturn();

        UserLoginResponseDto refreshResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), UserLoginResponseDto.class);

        assertThat(refreshResponse.token()).isNotBlank();
        assertThat(refreshResponse.refreshToken()).isNotEqualTo(loginResponse.refreshToken());
    }

    @Test
    @DisplayName("POST /api/auth/refresh: rejects a refresh token that was already used once")
    void refresh_reusedToken_isRejected() throws Exception {
        UserLoginResponseDto loginResponse = loginAsExistingUser();
        RefreshTokenRequestDto refreshRequest =
                new RefreshTokenRequestDto(loginResponse.refreshToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout: revokes the refresh token so it can no longer be used")
    void logout_revokesToken() throws Exception {
        UserLoginResponseDto loginResponse = loginAsExistingUser();
        RefreshTokenRequestDto request = new RefreshTokenRequestDto(loginResponse.refreshToken());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    private UserLoginResponseDto loginAsExistingUser() throws Exception {
        UserLoginRequestDto loginRequest = new UserLoginRequestDto("patient@test.com", "password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), UserLoginResponseDto.class);
    }
}
