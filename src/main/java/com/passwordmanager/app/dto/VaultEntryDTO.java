package com.passwordmanager.app.dto;

import com.passwordmanager.app.entity.VaultEntry;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VaultEntryDTO {

    private Long id;

    @NotBlank(message = "Account name is required")
    @Size(max = 100)
    private String accountName;

    @Size(max = 255)
    private String websiteUrl;

    @Size(max = 100)
    private String accountUsername;

    @NotBlank(message = "Password is required")
    private String password; // plain-text in DTO, encrypted before storage

    private VaultEntry.Category category = VaultEntry.Category.OTHER;

    @Size(max = 1000)
    private String notes;

    private boolean favorite;

    // Read-only: populated when viewing an entry
    private String createdAt;
    private String updatedAt;
    private String strengthLabel;
    private int strengthScore;
}
