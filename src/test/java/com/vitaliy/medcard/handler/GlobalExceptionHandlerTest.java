package com.vitaliy.medcard.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitaliy.medcard.exception.CareLinkAlreadyExistsException;
import com.vitaliy.medcard.exception.DocumentNotFoundException;
import com.vitaliy.medcard.exception.FileStorageException;
import com.vitaliy.medcard.exception.InvalidCredentialsException;
import com.vitaliy.medcard.exception.ShareLinkExpiredException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("each domain exception group maps to its HTTP status and keeps its message")
    void domainExceptions_mapToTheirStatus() {
        assertResponse(handler.handleNotFound(new DocumentNotFoundException("Document not found")),
                HttpStatus.NOT_FOUND, "Document not found");
        assertResponse(handler.handleUnauthorized(new InvalidCredentialsException("Bad login")),
                HttpStatus.UNAUTHORIZED, "Bad login");
        assertResponse(handler.handleConflict(new CareLinkAlreadyExistsException("Duplicate")),
                HttpStatus.CONFLICT, "Duplicate");
        assertResponse(handler.handleGone(new ShareLinkExpiredException("Expired")),
                HttpStatus.GONE, "Expired");
    }

    @Test
    @DisplayName("a storage failure is 500 without leaking the internal storage key")
    void infrastructureFailure_hidesInternalDetails() {
        ResponseEntity<Object> response = handler.handleInfrastructureFailure(
                new FileStorageException("File not found: 4f1c_uuid_blood-test.pdf"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(errorOf(response)).doesNotContain("4f1c_uuid_blood-test.pdf");
    }

    @Test
    @DisplayName("a unique-constraint race becomes 409 without leaking SQL details")
    void dataIntegrityViolation_isConflict() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException(
                "Duplicate entry '1-2' for key 'care_links.uq_care_links_doctor_patient'");

        ResponseEntity<Object> response = handler.handleDataIntegrityViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(errorOf(response))
                .doesNotContain("Duplicate entry")
                .doesNotContain("uq_care_links");
    }

    private void assertResponse(
            ResponseEntity<Object> response, HttpStatus status, String message) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(errorOf(response)).isEqualTo(message);
        assertThat(((Map<?, ?>) response.getBody()).get("timestamp")).isNotNull();
    }

    private String errorOf(ResponseEntity<Object> response) {
        return ((Map<?, ?>) response.getBody()).get("error").toString();
    }
}
