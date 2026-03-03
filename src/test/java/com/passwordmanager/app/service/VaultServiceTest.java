package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.repository.IVaultEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

    private VaultService vaultService;

    @Mock
    private IVaultEntryRepository vaultRepo;
    @Mock
    private IEncryptionService enc;

    @BeforeEach
    void setUp() {
        vaultService = new VaultService(vaultRepo, enc);
    }

    @Test
    void testAddEntry() {
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setAccountName("T");
        dto.setPassword("raw");
        when(enc.encrypt("raw")).thenReturn("encrypted");
        when(vaultRepo.save(any())).thenAnswer(i -> i.getArguments()[0]);

        VaultEntry r = vaultService.addEntry(new User(), dto);
        assertEquals("encrypted", r.getEncryptedPassword());
        verify(enc).encrypt("raw");
    }

    @Test
    void testGetEntryMasked() {
        VaultEntry e = new VaultEntry();
        e.setId(1L);
        e.setAccountName("T");
        e.setCreatedAt(LocalDateTime.now());
        when(vaultRepo.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(e));
        assertEquals("••••••••", vaultService.getEntryMasked(1L, 1L).getPassword());
    }

    @Test
    void testGetEntryWithDecryptedPassword() {
        VaultEntry e = new VaultEntry();
        e.setId(1L);
        e.setEncryptedPassword("enc");
        when(vaultRepo.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(e));
        when(enc.decrypt("enc")).thenReturn("dec");
        assertEquals("dec", vaultService.getEntryWithDecryptedPassword(1L, 1L).getPassword());
    }
}