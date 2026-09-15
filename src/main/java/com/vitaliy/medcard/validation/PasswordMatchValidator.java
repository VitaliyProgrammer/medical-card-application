package com.vitaliy.medcard.validation;

import com.vitaliy.medcard.dto.UserRegistrationRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchValidator implements
        ConstraintValidator<PasswordMatch, UserRegistrationRequestDto> {

    @Override
    public boolean isValid(UserRegistrationRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        if (dto.getPassword() == null || dto.getRepeatPassword() == null) {
            return true;
        }

        return dto.getPassword().equals(dto.getRepeatPassword());
    }
}
