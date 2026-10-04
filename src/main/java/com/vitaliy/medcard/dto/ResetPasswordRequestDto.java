package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.validation.ResetPasswordMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ResetPasswordMatch
public record ResetPasswordRequestDto(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long!")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).+$",
                message = "Password must contain at least one uppercase letter, "
                        + "one lowercase letter, one digit, and one special character!")
        String newPassword,

        @NotBlank
        String confirmPassword
) {
}
