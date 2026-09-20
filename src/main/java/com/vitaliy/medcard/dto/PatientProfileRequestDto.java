package com.vitaliy.medcard.dto;

import jakarta.validation.constraints.Past;
import java.time.LocalDate;
import lombok.Data;

@Data
public class PatientProfileRequestDto {

    @Past(message = "Date of birth must be in the past!")
    private LocalDate dateOfBirth;

    private String bloodGroup;

    private String address;

    private String emergencyContactName;

    private String emergencyContactPhone;
}
