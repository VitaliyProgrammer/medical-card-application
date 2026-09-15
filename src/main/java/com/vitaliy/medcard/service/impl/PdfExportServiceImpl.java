package com.vitaliy.medcard.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.vitaliy.medcard.exception.PdfGenerationException;
import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.Condition;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.Visit;
import com.vitaliy.medcard.service.PdfExportService;
import com.vitaliy.medcard.util.AgeCalculator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class PdfExportServiceImpl implements PdfExportService {

    private static final String TEMPLATE_NAME = "patient-card";

    private final TemplateEngine templateEngine;

    @Override
    public byte[] renderPatientCard(
            PatientProfile patient, List<Allergy> allergies,
            List<Condition> conditions, List<Visit> visits) {
        String html = renderHtml(patient, allergies, conditions, visits);
        return renderPdf(html);
    }

    private String renderHtml(
            PatientProfile patient, List<Allergy> allergies,
            List<Condition> conditions, List<Visit> visits) {
        Context context = new Context();
        context.setVariable("patientFullName", patient.getUser().getFullName());
        context.setVariable("dateOfBirth", patient.getDateOfBirth());
        context.setVariable("age", AgeCalculator.calculateAge(patient.getDateOfBirth()));
        context.setVariable("bloodGroup", patient.getBloodGroup());
        context.setVariable("emergencyContactName", patient.getEmergencyContactName());
        context.setVariable("emergencyContactPhone", patient.getEmergencyContactPhone());
        context.setVariable("allergies", allergies);
        context.setVariable("conditions", conditions);
        context.setVariable("visits", visits);
        context.setVariable("generatedAt", LocalDateTime.now());

        return templateEngine.process(TEMPLATE_NAME, context);
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new PdfGenerationException("Failed to generate the patient card PDF!");
        }
    }
}
