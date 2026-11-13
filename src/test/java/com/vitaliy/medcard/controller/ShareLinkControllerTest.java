package com.vitaliy.medcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.dto.ShareLinkResponseDto;
import com.vitaliy.medcard.model.CareLink;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.CareLinkRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.ShareLinkRepository;
import com.vitaliy.medcard.repository.UserRepository;
import com.vitaliy.medcard.security.JwtUtil;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class ShareLinkControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private CareLinkRepository careLinkRepository;

    @Autowired
    private ShareLinkRepository shareLinkRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User doctor;

    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        doctor = saveUser("doctor@test.com", "Dr. House", UserRole.DOCTOR);
        User patientUser = saveUser("patient@test.com", "Jane Patient", UserRole.PATIENT);

        patientProfile = new PatientProfile();
        patientProfile.setUser(patientUser);
        patientProfile.setCreatedAt(LocalDateTime.now());
        patientProfile.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(patientProfile);

        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patientProfile);
        careLink.setAssignedAt(LocalDateTime.now());
        careLinkRepository.save(careLink);
    }

    @Test
    @DisplayName("a share link exports the card publicly until the doctor revokes it")
    void createExportRevoke() throws Exception {
        ShareLinkResponseDto shareLink = createShareLink(doctor);

        assertThat(shareLinkRepository.findById(shareLink.id()).orElseThrow().getTokenHash())
                .isNotEqualTo(shareLink.token());

        mockMvc.perform(get("/api/share-links/{token}/export", shareLink.token()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/share-links/{id}", shareLink.id())
                        .header("Authorization", bearerToken(doctor)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/share-links/{token}/export", shareLink.token()))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("DELETE /api/share-links/{id}: another doctor can't revoke someone else's link")
    void revoke_byAnotherDoctor_notFound() throws Exception {
        ShareLinkResponseDto shareLink = createShareLink(doctor);
        User otherDoctor = saveUser("other-doctor@test.com", "Dr. Wilson", UserRole.DOCTOR);

        mockMvc.perform(delete("/api/share-links/{id}", shareLink.id())
                        .header("Authorization", bearerToken(otherDoctor)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/share-links/{token}/export", shareLink.token()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/share-links/{token}/export: an unknown token is 404")
    void export_unknownToken_notFound() throws Exception {
        mockMvc.perform(get("/api/share-links/{token}/export", "not-a-real-token"))
                .andExpect(status().isNotFound());
    }

    private ShareLinkResponseDto createShareLink(User currentDoctor) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/patients/{id}/share-links", patientProfile.getId())
                                .header("Authorization", bearerToken(currentDoctor))
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsString(), ShareLinkResponseDto.class);
    }

    private User saveUser(String email, String fullName, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed");
        user.setFullName(fullName);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
