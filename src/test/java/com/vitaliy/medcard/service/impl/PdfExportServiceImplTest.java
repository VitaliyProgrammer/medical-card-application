package com.vitaliy.medcard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.User;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

@ExtendWith(MockitoExtension.class)
class PdfExportServiceImplTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private PdfExportServiceImpl pdfExportService;

    private PatientProfile patient;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setFullName("Jane Patient");

        patient = new PatientProfile();
        patient.setUser(user);
    }

    @Test
    @DisplayName("renderPatientCard: turns the rendered HTML into real PDF bytes")
    void renderPatientCard_success() {
        String minimalXhtml = "<html><head><meta charset=\"UTF-8\"/></head>"
                + "<body><p>Jane Patient</p></body></html>";

        when(templateEngine.process(org.mockito.ArgumentMatchers.eq("patient-card"), any(IContext.class)))
                .thenReturn(minimalXhtml);

        byte[] pdf = pdfExportService.renderPatientCard(patient, List.of(), List.of(), List.of());

        assertThat(pdf).isNotEmpty();
        String header = new String(pdf, 0, 4, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF");
    }
}
