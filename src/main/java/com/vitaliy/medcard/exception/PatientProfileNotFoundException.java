package com.vitaliy.medcard.exception;

public class PatientProfileNotFoundException extends RuntimeException {

    public PatientProfileNotFoundException(String message) {
        super(message);
    }
}
