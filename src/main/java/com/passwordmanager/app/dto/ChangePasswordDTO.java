package com.passwordmanager.app.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm the new password")
    private String confirmNewPassword;
}
