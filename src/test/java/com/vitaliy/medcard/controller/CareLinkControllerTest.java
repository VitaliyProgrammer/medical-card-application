package com.vitaliy.medcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.dto.CareLinkRequestDto;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class CareLinkControllerTest extends IntegrationTestBase {

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

    private User doctor;

    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        doctor = new User();
        doctor.setEmail("doctor@test.com");
        doctor.setPassword("hashed");
        doctor.setFullName("Dr. House");
        doctor.setRole(UserRole.DOCTOR);
        doctor.setCreatedAt(LocalDateTime.now());
        userRepository.save(doctor);

        User patientUser = new User();
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
    @DisplayName("POST /api/care-links: a doctor can take a patient under their care")
    void assignPatient_success() throws Exception {
        CareLinkRequestDto request = new CareLinkRequestDto(patientProfile.getId());

        mockMvc.perform(post("/api/care-links")
                        .header("Authorization", bearerToken(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/care-links: rejects assigning the same patient twice")
    void assignPatient_duplicate() throws Exception {
        CareLinkRequestDto request = new CareLinkRequestDto(patientProfile.getId());

        mockMvc.perform(post("/api/care-links")
                        .header("Authorization", bearerToken(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/care-links")
                        .header("Authorization", bearerToken(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/care-links: a patient can't assign patients to doctors")
    void assignPatient_forbiddenForPatientRole() throws Exception {
        CareLinkRequestDto request = new CareLinkRequestDto(patientProfile.getId());
        User patientUser = patientProfile.getUser();

        mockMvc.perform(post("/api/care-links")
                        .header("Authorization", bearerToken(patientUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/care-links/my-patients: returns only patients assigned to me")
    void getMyPatients_success() throws Exception {
        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patientProfile);
        careLink.setAssignedAt(LocalDateTime.now());
        careLinkRepository.save(careLink);

        MvcResult result = mockMvc.perform(get("/api/care-links/my-patients")
                        .header("Authorization", bearerToken(doctor)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("content");

        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("fullName").asText()).isEqualTo("Jane Patient");
    }

    @Test
    @DisplayName("GET /api/care-links/my-patients: search filters by patient full name")
    void getMyPatients_searchFiltersOut() throws Exception {
        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patientProfile);
        careLink.setAssignedAt(LocalDateTime.now());
        careLinkRepository.save(careLink);

        MvcResult result = mockMvc.perform(get("/api/care-links/my-patients")
                        .header("Authorization", bearerToken(doctor))
                        .param("search", "Nobody"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("content");

        assertThat(content).isEmpty();
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
