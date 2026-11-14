package com.vitaliy.medcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.dto.DocumentResponseDto;
import com.vitaliy.medcard.model.CareLink;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.CareLinkRepository;
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
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class DocumentControllerTest extends IntegrationTestBase {

    private static final byte[] PDF_CONTENT = "%PDF-1.7 test content".getBytes();

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
                "file", "blood-test.pdf", "application/pdf", PDF_CONTENT);

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
    @DisplayName("POST /api/patients/me/documents: rejects a non-PDF file sent as application/pdf")
    void uploadMyDocument_spoofedContentType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "invoice.pdf", "application/pdf", "MZ executable bytes".getBytes());

        mockMvc.perform(multipart("/api/patients/me/documents")
                        .file(file)
                        .param("documentType", "OTHER")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/patients/me/documents/{id}/download: encodes the file name safely")
    void downloadMyDocument_contentDispositionIsEncoded() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "аналіз \"крові\".pdf", "application/pdf", PDF_CONTENT);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/patients/me/documents")
                        .file(file)
                        .param("documentType", "LAB_RESULT")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isCreated())
                .andReturn();
        DocumentResponseDto uploaded = objectMapper.readValue(
                uploadResult.getResponse().getContentAsString(), DocumentResponseDto.class);

        MvcResult downloadResult = mockMvc.perform(
                        get("/api/patients/me/documents/{id}/download", uploaded.id())
                                .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isOk())
                .andReturn();

        String header = downloadResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(header)
                .startsWith("attachment;")
                .contains("filename*=UTF-8''")
                .doesNotContain("\"крові\"");
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
                "file", "blood-test.pdf", "application/pdf", PDF_CONTENT);

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
                "file", "blood-test.pdf", "application/pdf", PDF_CONTENT);

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

    @Test
    @DisplayName("GET /api/patients/{id}/documents/{documentId}/download: "
            + "an assigned doctor downloads their patient's document")
    void downloadPatientDocument_success() throws Exception {
        User doctor = saveUser("doctor@test.com", "Dr. House", UserRole.DOCTOR);
        assignToDoctor(doctor, patientProfile);
        DocumentResponseDto uploaded = uploadAs(patientUser);

        mockMvc.perform(get("/api/patients/{id}/documents/{documentId}/download",
                        patientProfile.getId(), uploaded.id())
                        .header("Authorization", bearerToken(doctor)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{id}/documents/{documentId}/download: "
            + "a doctor can't download another patient's document via their own patient's id")
    void downloadPatientDocument_documentOfAnotherPatient() throws Exception {
        User doctor = saveUser("doctor@test.com", "Dr. House", UserRole.DOCTOR);
        assignToDoctor(doctor, patientProfile);

        User otherPatientUser = saveUser("other@test.com", "John Other", UserRole.PATIENT);
        savePatientProfile(otherPatientUser);
        DocumentResponseDto otherPatientDocument = uploadAs(otherPatientUser);

        mockMvc.perform(get("/api/patients/{id}/documents/{documentId}/download",
                        patientProfile.getId(), otherPatientDocument.id())
                        .header("Authorization", bearerToken(doctor)))
                .andExpect(status().isNotFound());
    }

    private DocumentResponseDto uploadAs(User user) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "blood-test.pdf", "application/pdf", PDF_CONTENT);

        MvcResult result = mockMvc.perform(multipart("/api/patients/me/documents")
                        .file(file)
                        .param("documentType", "LAB_RESULT")
                        .header("Authorization", bearerToken(user)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsString(), DocumentResponseDto.class);
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

    private void savePatientProfile(User user) {
        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(profile);
    }

    private void assignToDoctor(User doctor, PatientProfile patient) {
        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patient);
        careLink.setAssignedAt(LocalDateTime.now());
        careLinkRepository.save(careLink);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
