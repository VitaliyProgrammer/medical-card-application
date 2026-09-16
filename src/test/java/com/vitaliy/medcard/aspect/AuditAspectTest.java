package com.vitaliy.medcard.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitaliy.medcard.IntegrationTestBase;
import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import com.vitaliy.medcard.exception.RegistrationException;
import com.vitaliy.medcard.model.AuditEntry;
import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.repository.AuditEntryRepository;
import com.vitaliy.medcard.service.AuthenticationService;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional(isolation = Isolation.READ_COMMITTED)
class AuditAspectTest extends IntegrationTestBase {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private AuditEntryRepository auditEntryRepository;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void successfulRegistration_writesASuccessAuditEntry() {
        UserRegistrationRequestDto request = new UserRegistrationRequestDto();
        request.setEmail("audited.patient@test.com");
        request.setPassword("password123");
        request.setRepeatPassword("password123");
        request.setFullName("Audited Patient");
        request.setRole(UserRole.PATIENT);

        authenticationService.register(request);

        List<AuditEntry> entries = auditEntryRepository.findAll();
        AuditEntry entry = entries.stream()
                .filter(e -> "REGISTER_USER".equals(e.getAction()))
                .max(Comparator.comparing(AuditEntry::getId))
                .orElseThrow();

        assertThat(entry.getOutcome()).isEqualTo("SUCCESS");
        assertThat(entry.getMethodName()).contains("register");

        double count = meterRegistry.counter("audit_entries_total",
                "action", "REGISTER_USER", "outcome", "SUCCESS").count();
        assertThat(count).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void failedRegistration_writesAFailureAuditEntry() {
        UserRegistrationRequestDto request = new UserRegistrationRequestDto();
        request.setEmail("blocked.admin@test.com");
        request.setPassword("password123");
        request.setRepeatPassword("password123");
        request.setFullName("Blocked Admin");
        request.setRole(UserRole.ADMIN);

        assertThrows(RegistrationException.class, () -> authenticationService.register(request));

        List<AuditEntry> entries = auditEntryRepository.findAll();
        AuditEntry entry = entries.stream()
                .filter(e -> "REGISTER_USER".equals(e.getAction()))
                .max(Comparator.comparing(AuditEntry::getId))
                .orElseThrow();

        assertThat(entry.getOutcome()).isEqualTo("FAILURE");
    }
}
