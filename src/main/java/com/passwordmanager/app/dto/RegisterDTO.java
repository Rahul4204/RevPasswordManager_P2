package com.passwordmanager.app.dto;

import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class RegisterDTO {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^\\w+$", message = "Username may only contain letters, numbers, and underscores")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Master password is required")
    @Size(min = 8, message = "Master password must be at least 8 characters")
    private String masterPassword;

    @NotBlank(message = "Please confirm your master password")
    private String confirmPassword;

    @NotNull
    @Size(min = 3, message = "At least 3 security questions are required")
    @Valid
    private List<SecurityQuestionDTO> securityQuestions;
}
