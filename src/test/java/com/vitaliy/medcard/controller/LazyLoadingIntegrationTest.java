package com.vitaliy.medcard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.model.CareLink;
import com.vitaliy.medcard.model.Document;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.Visit;
import com.vitaliy.medcard.model.status.DocumentType;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.CareLinkRepository;
import com.vitaliy.medcard.repository.DocumentRepository;
import com.vitaliy.medcard.repository.PatientProfileRepository;
import com.vitaliy.medcard.repository.UserRepository;
import com.vitaliy.medcard.repository.VisitRepository;
import com.vitaliy.medcard.security.JwtUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LazyLoadingIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private CareLinkRepository careLinkRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User doctor;

    private User patientUser;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        doctor = saveUser("lazy-doctor-" + suffix + "@test.com", "Dr. Lazy", UserRole.DOCTOR);
        patientUser = saveUser(
                "lazy-patient-" + suffix + "@test.com", "Lazy Patient", UserRole.PATIENT);

        patient = new PatientProfile();
        patient.setUser(patientUser);
        patient.setDateOfBirth(LocalDate.of(1990, 5, 17));
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(patient);

        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patient);
        careLink.setAssignedAt(LocalDateTime.now());
        careLinkRepository.save(careLink);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setDoctor(doctor);
        visit.setVisitDate(LocalDateTime.now().minusDays(1));
        visit.setDiagnosis("Seasonal flu");
        visitRepository.save(visit);

        Document document = new Document();
        document.setPatient(patient);
        document.setUploadedBy(patientUser);
        document.setDocumentType(DocumentType.LAB_RESULT);
        document.setOriginalFileName("blood-test.pdf");
        document.setStoredFileName("lazy-" + suffix + ".pdf");
        document.setContentType("application/pdf");
        document.setFileSize(42L);
        document.setUploadedAt(LocalDateTime.now());
        documentRepository.save(document);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllByIdInBatch(List.of(doctor.getId(), patientUser.getId()));
    }

    @Test
    @DisplayName("patient profile resolves its lazy user outside a test transaction")
    void myProfile() throws Exception {
        mockMvc.perform(get("/api/patients/me").header("Authorization", bearer(patientUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Lazy Patient"));
    }

    @Test
    @DisplayName("doctor's patient search resolves patient.user for every row")
    void myPatients() throws Exception {
        mockMvc.perform(get("/api/care-links/my-patients").header("Authorization", bearer(doctor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Lazy Patient"));
    }

    @Test
    @DisplayName("patient's doctor list resolves doctor and patient.user")
    void myDoctors() throws Exception {
        mockMvc.perform(get("/api/patients/me/care-links")
                        .header("Authorization", bearer(patientUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorFullName").value("Dr. Lazy"))
                .andExpect(jsonPath("$[0].patientFullName").value("Lazy Patient"));
    }

    @Test
    @DisplayName("visit history resolves the lazy doctor of each visit")
    void visits() throws Exception {
        mockMvc.perform(get("/api/patients/{id}/visits", patient.getId())
                        .header("Authorization", bearer(doctor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].doctorFullName").value("Dr. Lazy"));
    }

    @Test
    @DisplayName("document list resolves the lazy uploader of each document")
    void documents() throws Exception {
        mockMvc.perform(get("/api/patients/me/documents")
                        .header("Authorization", bearer(patientUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].uploadedByFullName").value("Lazy Patient"));
    }

    @Test
    @DisplayName("PDF export walks every lazy association of the card")
    void pdfExport() throws Exception {
        mockMvc.perform(get("/api/patients/{id}/export", patient.getId())
                        .header("Authorization", bearer(doctor)))
                .andExpect(status().isOk());
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

    private String bearer(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
