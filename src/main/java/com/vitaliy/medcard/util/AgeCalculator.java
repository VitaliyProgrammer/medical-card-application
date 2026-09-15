package com.vitaliy.medcard.util;

import java.time.LocalDate;
import java.time.Period;

public final class AgeCalculator {

    private AgeCalculator() {
    }

    public static Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
