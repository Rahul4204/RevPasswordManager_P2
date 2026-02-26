package com.passwordmanager.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pm_vault_entries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "vault_seq")
    @SequenceGenerator(name = "vault_seq", sequenceName = "pm_vault_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @Column(name = "account_username", length = 100)
    private String accountUsername;

    // Stored AES-encrypted
    @Column(name = "encrypted_password", nullable = false, length = 500)
    private String encryptedPassword;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    @Builder.Default
    private Category category = Category.OTHER;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private boolean favorite = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Category {
        SOCIAL_MEDIA, BANKING, EMAIL, SHOPPING, WORK, OTHER
    }
}
