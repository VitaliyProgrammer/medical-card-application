package com.vitaliy.medcard.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("a unique-constraint race becomes 409 without leaking SQL details")
    void dataIntegrityViolation_isConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry '1-2' for key 'care_links.uq_care_links_doctor_patient'");

        ResponseEntity<Object> response = handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(((Map<?, ?>) response.getBody()).get("error").toString())
                .doesNotContain("Duplicate entry")
                .doesNotContain("uq_care_links");
    }
}
