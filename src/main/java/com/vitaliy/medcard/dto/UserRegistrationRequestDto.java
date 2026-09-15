package com.vitaliy.medcard.dto;

import com.vitaliy.medcard.model.status.UserRole;
import com.vitaliy.medcard.validation.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@PasswordMatch
public class UserRegistrationRequestDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    @Size(min = 6, max = 20, message = "Password must be at least 6 characters long!")
    private String password;

    @NotBlank
    private String repeatPassword;

    @NotBlank
    private String fullName;

    @NotNull(message = "Role must be either PATIENT or DOCTOR!")
    private UserRole role;
}
