package com.vitaliy.medcard.service;

import com.vitaliy.medcard.model.User;

public interface PatientCardExportService {

    byte[] exportMyCard(User currentUser);

    byte[] exportCardForDoctor(User currentDoctor, Long patientId);

    /** No ownership check - the caller must already have authorized access to this patient. */
    byte[] exportCardById(Long patientId);
}
