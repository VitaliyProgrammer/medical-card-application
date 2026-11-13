package com.vitaliy.medcard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class CareLinkControllerTest extends IntegrationTestBase {

    private static final String MY_CARE_LINKS = "/api/patients/me/care-links";

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

    private User patientUser;

    private PatientProfile patientProfile;

    @BeforeEach
    void setUp() {
        doctor = saveUser("doctor@test.com", "Dr. House", UserRole.DOCTOR);
        patientUser = saveUser("patient@test.com", "Jane Patient", UserRole.PATIENT);
        patientProfile = savePatientProfile(patientUser);
    }

    @Test
    @DisplayName("POST /api/patients/me/care-links: a patient grants a doctor access")
    void grantDoctorAccess_success() throws Exception {
        grantAccess(patientUser, doctor.getEmail()).andExpect(status().isCreated());

        assertThat(careLinkRepository.existsByDoctorIdAndPatientId(
                doctor.getId(), patientProfile.getId())).isTrue();
    }

    @Test
    @DisplayName("POST /api/patients/me/care-links: rejects granting the same doctor twice")
    void grantDoctorAccess_duplicate() throws Exception {
        grantAccess(patientUser, doctor.getEmail()).andExpect(status().isCreated());
        grantAccess(patientUser, doctor.getEmail()).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/patients/me/care-links: an email of a non-doctor is not found")
    void grantDoctorAccess_userIsNotDoctor() throws Exception {
        User otherPatient = saveUser("other@test.com", "John Other", UserRole.PATIENT);

        grantAccess(patientUser, otherPatient.getEmail()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/patients/me/care-links: a doctor can't grant access to themselves")
    void grantDoctorAccess_forbiddenForDoctor() throws Exception {
        grantAccess(doctor, doctor.getEmail()).andExpect(status().isForbidden());

        assertThat(careLinkRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/care-links: the old doctor self-assignment endpoint no longer exists")
    void doctorSelfAssignmentEndpoint_removed() throws Exception {
        mockMvc.perform(post("/api/care-links")
                        .header("Authorization", bearerToken(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\": " + patientProfile.getId() + "}"))
                .andExpect(status().isNotFound());

        assertThat(careLinkRepository.count()).isZero();
    }

    @Test
    @DisplayName("GET /api/patients/me/care-links: lists doctors with access to my card")
    void getMyDoctors_success() throws Exception {
        saveCareLink(doctor, patientProfile);

        MvcResult result = mockMvc.perform(get(MY_CARE_LINKS)
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString());

        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("doctorFullName").asText()).isEqualTo("Dr. House");
    }

    @Test
    @DisplayName("DELETE /api/patients/me/care-links/{id}: a patient revokes a doctor's access")
    void revokeDoctorAccess_success() throws Exception {
        CareLink careLink = saveCareLink(doctor, patientProfile);

        mockMvc.perform(delete(MY_CARE_LINKS + "/{id}", careLink.getId())
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isNoContent());

        assertThat(careLinkRepository.findById(careLink.getId())).isEmpty();
    }

    @Test
    @DisplayName("DELETE /api/patients/me/care-links/{id}: can't revoke another patient's link")
    void revokeDoctorAccess_careLinkOfAnotherPatient() throws Exception {
        User otherPatientUser = saveUser("other@test.com", "John Other", UserRole.PATIENT);
        PatientProfile otherPatient = savePatientProfile(otherPatientUser);
        CareLink otherPatientLink = saveCareLink(doctor, otherPatient);

        mockMvc.perform(delete(MY_CARE_LINKS + "/{id}", otherPatientLink.getId())
                        .header("Authorization", bearerToken(patientUser)))
                .andExpect(status().isNotFound());

        assertThat(careLinkRepository.findById(otherPatientLink.getId())).isPresent();
    }

    @Test
    @DisplayName("GET /api/care-links/my-patients: returns only patients who granted me access")
    void getMyPatients_success() throws Exception {
        saveCareLink(doctor, patientProfile);

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
        saveCareLink(doctor, patientProfile);

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

    private ResultActions grantAccess(User currentUser, String doctorEmail) throws Exception {
        return mockMvc.perform(post(MY_CARE_LINKS)
                .header("Authorization", bearerToken(currentUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CareLinkRequestDto(doctorEmail))));
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

    private PatientProfile savePatientProfile(User user) {
        PatientProfile profile = new PatientProfile();
        profile.setUser(user);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return patientProfileRepository.save(profile);
    }

    private CareLink saveCareLink(User doctor, PatientProfile patient) {
        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patient);
        careLink.setAssignedAt(LocalDateTime.now());
        return careLinkRepository.save(careLink);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
