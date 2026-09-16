package com.vitaliy.medcard.service;

import com.vitaliy.medcard.model.User;

public interface PatientCardExportService {

    byte[] exportMyCard(User currentUser);

    byte[] exportCardForDoctor(User currentDoctor, Long patientId);

    byte[] exportCardById(Long patientId);
}
