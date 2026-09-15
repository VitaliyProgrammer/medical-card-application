package com.vitaliy.medcard.service;

import com.vitaliy.medcard.model.Allergy;
import com.vitaliy.medcard.model.Condition;
import com.vitaliy.medcard.model.PatientProfile;
import com.vitaliy.medcard.model.Visit;
import java.util.List;

public interface PdfExportService {

    byte[] renderPatientCard(
            PatientProfile patient, List<Allergy> allergies,
            List<Condition> conditions, List<Visit> visits);
}
