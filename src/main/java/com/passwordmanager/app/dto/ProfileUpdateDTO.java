package com.passwordmanager.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ProfileUpdateDTO {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email
    private String email;

    @Size(max = 20)
    private String phone;

    private MultipartFile profilePhoto;
}
