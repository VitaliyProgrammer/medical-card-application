package com.vitaliy.medcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.dto.DocumentResponseDto;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.UserRepository;
import com.vitaliy.medcard.security.JwtUtil;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class DocumentControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User patientUser;

    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setEmail("patient@test.com");
        patientUser.setPassword("hashed");
        patientUser.setFullName("Jane Patient");
        patientUser.setRole(UserRole.PATIENT);
        patientUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(patientUser);

        patientProfile = new PatientProfile();
        patientProfile.setUser(patientUser);
        patientProfile.setCreatedAt(LocalDateTime.now());
        patientProfile.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(patientProfile);
    }

    @Test
    @DisplayName("POST /api/patients/me/documents: uploads a document to my own card")
    void uploadMyDocument_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blood-test.pdf", "application/pdf", "test content".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/patients/me/documents")
                        .file(file)
                        .param("documentType", "LAB_RESULT")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentResponseDto response = objectMapper.readValue(
                result.getResponse().getContentAsString(), DocumentResponseDto.class);

        assertThat(response.originalFileName()).isEqualTo("blood-test.pdf");
    }

    @Test
    @DisplayName("POST /api/patients/me/documents: rejects an unsupported file type")
    void uploadMyDocument_unsupportedType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload", "test content".getBytes());

        mockMvc.perform(multipart("/api/patients/me/documents")
                        .file(file)
                        .param("documentType", "OTHER")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/patients/me/documents: lists my uploaded documents")
    void getMyDocuments_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blood-test.pdf", "application/pdf", "test content".getBytes());

        mockMvc.perform(multipart("/api/patients/me/documents")
                        .file(file)
                        .param("documentType", "LAB_RESULT")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/patients/me/documents")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/patients/me/documents/{id}: removes an uploaded document")
    void deleteMyDocument_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blood-test.pdf", "application/pdf", "test content".getBytes());

        MvcResult uploadResult = mockMvc.perform(multipart("/api/patients/me/documents")
                        .file(file)
                        .param("documentType", "LAB_RESULT")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isCreated())
                .andReturn();

        DocumentResponseDto uploaded = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(), DocumentResponseDto.class);

        mockMvc.perform(delete("/api/patients/me/documents/{id}", uploaded.id())
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isNoContent());
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
