package com.vitaliy.medcard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitaliy.medcard.IntegrationTestBase;
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
class UserRepositoryTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("doctor@test.com");
        user.setPassword("hashed");
        user.setFullName("Dr. House");
        user.setRole(UserRole.DOCTOR);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
    }

    @Test
    @DisplayName("findByEmail: returns the user for an existing email")
    void findByEmail_existing() {
        var found = userRepository.findByEmail("doctor@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Dr. House");
    }

    @Test
    @DisplayName("findByEmail: is empty for an unknown email")
    void findByEmail_unknown() {
        assertThat(userRepository.findByEmail("nobody@test.com")).isEmpty();
    }

    @Test
    @DisplayName("existsByEmail: true for an existing email, false otherwise")
    void existsByEmail() {
        assertThat(userRepository.existsByEmail("doctor@test.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@test.com")).isFalse();
    }
}
