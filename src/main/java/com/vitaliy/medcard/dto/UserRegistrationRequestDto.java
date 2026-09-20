package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.validation.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PasswordMatch
public class UserRegistrationRequestDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long!")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).+$",
            message = "Password must contain at least one uppercase letter, "
                    + "one lowercase letter, one digit, and one special character!")
    private String password;

    @NotBlank
    private String repeatPassword;

    @NotBlank
    private String fullName;

    @NotNull(message = "Role must be either PATIENT or DOCTOR!")
    private UserRole role;
}
