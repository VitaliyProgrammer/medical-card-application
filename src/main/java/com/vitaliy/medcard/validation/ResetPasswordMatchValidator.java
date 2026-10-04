package com.vitaliy.medcard.validation;

import com.vitaliy.medcard.dto.ResetPasswordRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ResetPasswordMatchValidator implements
        ConstraintValidator<ResetPasswordMatch, ResetPasswordRequestDto> {

    @Override
    public boolean isValid(ResetPasswordRequestDto dto, ConstraintValidatorContext context) {
        if (dto == null) {
            return true;
        }

        if (dto.newPassword() == null || dto.confirmPassword() == null) {
            return true;
        }

        return dto.newPassword().equals(dto.confirmPassword());
    }
}
