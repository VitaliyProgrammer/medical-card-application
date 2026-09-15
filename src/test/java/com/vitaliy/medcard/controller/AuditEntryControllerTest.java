package com.vitaliy.medcard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
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
class AuditEntryControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private User admin;

    private User doctor;

    @BeforeEach
    void setUp() {
        admin = createUser("admin@test.com", UserRole.ADMIN);
        doctor = createUser("doctor@test.com", UserRole.DOCTOR);
    }

    @Test
    @DisplayName("GET /api/audit-entries: an admin can list the audit log")
    void getAuditEntries_admin() throws Exception {
        mockMvc.perform(get("/api/audit-entries")
                        .header("Authorization", bearerToken(admin)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/audit-entries: a non-admin is forbidden")
    void getAuditEntries_nonAdminForbidden() throws Exception {
        mockMvc.perform(get("/api/audit-entries")
                        .header("Authorization", bearerToken(doctor)))
                .andExpect(status().isForbidden());
    }

    private User createUser(String email, UserRole role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("hashed");
        user.setFullName("Test User");
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtUtil.generateToken(user.getEmail(), user.getRole().name());
    }
}
