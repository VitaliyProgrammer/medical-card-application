package com.vitaliy.medcard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import com.vitaliy.medcard.model.status.UserRole;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PatientProfileRepositoryTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    private User patientUser;

    @BeforeEach
    void setUp() {
        patientUser = new User();
        patientUser.setEmail("patient@test.com");
        patientUser.setPassword("hashed");
        patientUser.setFullName("Jane Patient");
        patientUser.setRole(UserRole.PATIENT);
        patientUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(patientUser);

        PatientProfile profile = new PatientProfile();
        profile.setUser(patientUser);
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(profile);
    }

    @Test
    @DisplayName("findByUserId: returns the profile linked to the given user")
    void findByUserId_existing() {
        var found = patientProfileRepository.findByUserId(patientUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("patient@test.com");
    }

    @Test
    @DisplayName("findByUserId: is empty for a user without a card")
    void findByUserId_noProfile() {
        assertThat(patientProfileRepository.findByUserId(999L)).isEmpty();
    }
}
