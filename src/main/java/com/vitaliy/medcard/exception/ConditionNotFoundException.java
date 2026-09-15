package com.vitaliy.medcard.exception;

public class ConditionNotFoundException extends RuntimeException {

    public ConditionNotFoundException(String message) {
        super(message);
    }
}
