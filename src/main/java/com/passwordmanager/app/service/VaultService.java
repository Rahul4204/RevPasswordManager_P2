package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.exception.ResourceNotFoundException;
import com.passwordmanager.app.repository.IVaultEntryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class VaultService implements IVaultService {

    private final IVaultEntryRepository vaultRepo;
    private final IEncryptionService encryptionService;

    public VaultService(IVaultEntryRepository vaultRepo, IEncryptionService encryptionService) {
        this.vaultRepo = vaultRepo;
        this.encryptionService = encryptionService;
    }

    @Override
    public VaultEntry addEntry(User user, VaultEntryDTO dto) {
        VaultEntry entry = VaultEntry.builder()
                .user(user)
                .accountName(dto.getAccountName())
                .accountUsername(dto.getAccountUsername())
                .websiteUrl(dto.getWebsiteUrl())
                .category(dto.getCategory() != null ? dto.getCategory() : VaultEntry.Category.OTHER)
                .notes(dto.getNotes())
                .encryptedPassword(encryptionService.encrypt(dto.getPassword()))
                .favorite(false)
                .build();
        return vaultRepo.save(entry);
    }

    @Override
    public VaultEntry updateEntry(Long userId, Long entryId, VaultEntryDTO dto) {
        VaultEntry entry = vaultRepo.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        entry.setAccountName(dto.getAccountName());
        entry.setAccountUsername(dto.getAccountUsername());
        entry.setWebsiteUrl(dto.getWebsiteUrl());
        entry.setCategory(dto.getCategory() != null ? dto.getCategory() : VaultEntry.Category.OTHER);
        entry.setNotes(dto.getNotes());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty() && !dto.getPassword().equals("********")) {
            entry.setEncryptedPassword(encryptionService.encrypt(dto.getPassword()));
        }
        return vaultRepo.save(entry);
    }

    @Override
    public void deleteEntry(Long userId, Long entryId) {
        VaultEntry entry = vaultRepo.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        vaultRepo.delete(entry);
    }

    @Override
    public void toggleFavorite(Long userId, Long entryId) {
        VaultEntry entry = vaultRepo.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        entry.setFavorite(!entry.isFavorite());
        vaultRepo.save(entry);
    }

    @Override
    public List<VaultEntryDTO> getAllEntries(Long userId, String search, String category, String sort) {
        List<VaultEntry> entries;
        if (search != null && !search.isEmpty()) {
            entries = vaultRepo.search(userId, search);
        } else {
            entries = vaultRepo.findByUserIdOrderByAccountNameAsc(userId);
        }

        return entries.stream()
                .filter(e -> category == null || category.equalsIgnoreCase("ALL")
                        || category.equalsIgnoreCase(e.getCategory().name()))
                .sorted((e1, e2) -> {
                    if ("recent".equalsIgnoreCase(sort)) {
                        return e2.getUpdatedAt() != null && e1.getUpdatedAt() != null
                                ? e2.getUpdatedAt().compareTo(e1.getUpdatedAt())
                                : 0;
                    }
                    return e1.getAccountName().compareToIgnoreCase(e2.getAccountName());
                })
                .map(this::mapToMaskedDto)
                .collect(Collectors.toList());
    }

    @Override
    public VaultEntryDTO getEntryWithDecryptedPassword(Long userId, Long entryId) {
        VaultEntry entry = vaultRepo.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        VaultEntryDTO dto = mapToDto(entry);
        dto.setPassword(encryptionService.decrypt(entry.getEncryptedPassword()));
        return dto;
    }

    @Override
    public VaultEntryDTO getEntryMasked(Long userId, Long entryId) {
        VaultEntry entry = vaultRepo.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Entry not found"));
        return mapToMaskedDto(entry);
    }

    @Override
    public List<VaultEntryDTO> getFavorites(Long userId) {
        return vaultRepo.findByUserIdAndFavoriteTrueOrderByAccountNameAsc(userId).stream()
                .map(this::mapToMaskedDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<VaultEntryDTO> getRecentEntries(Long userId, int limit) {
        return vaultRepo.findRecentByUserId(userId, PageRequest.of(0, limit)).stream()
                .map(this::mapToMaskedDto)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUser(Long userId) {
        return vaultRepo.countByUserId(userId);
    }

    @Override
    public List<VaultEntry> getAllRawEntries(Long userId) {
        return vaultRepo.findByUserIdOrderByAccountNameAsc(userId);
    }

    @Override
    public String decryptPassword(VaultEntry entry) {
        return encryptionService.decrypt(entry.getEncryptedPassword());
    }

    private VaultEntryDTO mapToMaskedDto(VaultEntry e) {
        VaultEntryDTO dto = mapToDto(e);
        dto.setPassword("••••••••");
        return dto;
    }

    private VaultEntryDTO mapToDto(VaultEntry e) {
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setId(e.getId());
        dto.setAccountName(e.getAccountName());
        dto.setAccountUsername(e.getAccountUsername());
        dto.setWebsiteUrl(e.getWebsiteUrl());
        dto.setCategory(e.getCategory());
        dto.setNotes(e.getNotes());
        dto.setFavorite(e.isFavorite());
        return dto;
    }
}
