package com.passwordmanager.app.mapper;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.service.IEncryptionService;
import org.springframework.stereotype.Component;

@Component
public class VaultEntryMapper {

    private final IEncryptionService encryptionService;

    public VaultEntryMapper(IEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public VaultEntryDTO toDTO(VaultEntry entity) {
        if (entity == null) {
            return null;
        }
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setId(entity.getId());
        dto.setAccountName(entity.getAccountName());
        dto.setAccountUsername(entity.getAccountUsername());
        dto.setWebsiteUrl(entity.getWebsiteUrl());
        dto.setCategory(entity.getCategory());
        dto.setNotes(entity.getNotes());
        dto.setFavorite(entity.isFavorite());
        return dto;
    }

    public VaultEntryDTO toDTOWithPassword(VaultEntry entity) {
        VaultEntryDTO dto = toDTO(entity);
        if (dto != null && entity.getEncryptedPassword() != null) {
            dto.setPassword(encryptionService.decrypt(entity.getEncryptedPassword()));
        }
        return dto;
    }

    public VaultEntry toEntity(VaultEntryDTO dto) {
        if (dto == null) {
            return null;
        }
        VaultEntry entity = new VaultEntry();
        entity.setId(dto.getId());
        entity.setAccountName(dto.getAccountName());
        entity.setAccountUsername(dto.getAccountUsername());
        entity.setWebsiteUrl(dto.getWebsiteUrl());
        entity.setCategory(dto.getCategory());
        entity.setNotes(dto.getNotes());
        entity.setFavorite(dto.isFavorite());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            entity.setEncryptedPassword(encryptionService.encrypt(dto.getPassword()));
        }
        return entity;
    }
}
