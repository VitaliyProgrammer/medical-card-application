package com.vitaliy.medcard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitaliy.medcard.IntegrationTestBase;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class PatientProfileControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private CareLinkRepository careLinkRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User patientUser;

    private User assignedDoctor;

    private User strangerDoctor;

    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        patientUser = createUser("patient@test.com", "Jane Patient", UserRole.PATIENT);
        assignedDoctor = createUser("assigned.doctor@test.com", "Dr. Assigned", UserRole.DOCTOR);
        strangerDoctor = createUser("stranger.doctor@test.com", "Dr. Stranger", UserRole.DOCTOR);

        patientProfile = new PatientProfile();
        patientProfile.setUser(patientUser);
        patientProfile.setCreatedAt(LocalDateTime.now());
        patientProfile.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(patientProfile);

        CareLink careLink = new CareLink();
        careLink.setDoctor(assignedDoctor);
        careLink.setPatient(patientProfile);
        careLink.setAssignedAt(LocalDateTime.now());
        careLinkRepository.save(careLink);
    }

    @Test
    @DisplayName("GET /api/patients/me: a patient can read their own card")
    void getMyProfile_success() throws Exception {
        mockMvc.perform(get("/api/patients/me")
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{id}: an assigned doctor can read the patient's card")
    void getPatientProfile_assignedDoctor() throws Exception {
        mockMvc.perform(get("/api/patients/{id}", patientProfile.getId())
                        .header("Authorization", bearerToken(assignedDoctor)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/patients/{id}: an unassigned doctor is forbidden")
    void getPatientProfile_unassignedDoctor() throws Exception {
        mockMvc.perform(get("/api/patients/{id}", patientProfile.getId())
                        .header("Authorization", bearerToken(strangerDoctor)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients/{id}: a patient can't view another patient's endpoint")
    void getPatientProfile_forbiddenForPatientRole() throws Exception {
        mockMvc.perform(get("/api/patients/{id}", patientProfile.getId())
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/patients/me: rejects a request without a token")
    void getMyProfile_noToken() throws Exception {
        mockMvc.perform(get("/api/patients/me"))
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email, String fullName, UserRole role) {
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
