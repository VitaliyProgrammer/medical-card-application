package com.vitaliy.medcard.dto;

import java.util.List;

public record PatientSummaryDto(
        Long id,
        String fullName,
        Integer age,
        List<String> allergies
) {
}
