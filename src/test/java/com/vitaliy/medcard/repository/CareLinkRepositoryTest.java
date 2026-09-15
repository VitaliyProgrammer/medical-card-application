package com.vitaliy.medcard.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.model.CareLink;
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
class CareLinkRepositoryTest extends IntegrationTestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private CareLinkRepository careLinkRepository;

    private User doctor;

    private PatientProfile patient;

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

        patient = new PatientProfile();
        patient.setUser(patientUser);
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());
        patientProfileRepository.save(patient);

        CareLink careLink = new CareLink();
        careLink.setDoctor(doctor);
        careLink.setPatient(patient);
        careLink.setAssignedAt(LocalDateTime.now());
        careLinkRepository.save(careLink);
    }

    @Test
    @DisplayName("existsByDoctorIdAndPatientId: true for an assigned pair")
    void existsByDoctorIdAndPatientId_assigned() {
        assertThat(careLinkRepository.existsByDoctorIdAndPatientId(doctor.getId(), patient.getId()))
                .isTrue();
    }

    @Test
    @DisplayName("existsByDoctorIdAndPatientId: false for an unassigned pair")
    void existsByDoctorIdAndPatientId_notAssigned() {
        assertThat(careLinkRepository.existsByDoctorIdAndPatientId(999L, patient.getId()))
                .isFalse();
    }
}
