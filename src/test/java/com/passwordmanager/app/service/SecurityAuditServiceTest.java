package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.AuditReport;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.repository.IVaultEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {

    private SecurityAuditService auditService;

    @Mock
    private IVaultEntryRepository vaultRepo;
    @Mock
    private IEncryptionService enc;
    @Mock
    private IPasswordGeneratorService gen;

    @BeforeEach
    void setUp() {
        auditService = new SecurityAuditService(vaultRepo, enc, gen);
    }

    @Test
    void testWeakPassword() {
        VaultEntry e = new VaultEntry();
        e.setId(1L);
        e.setAccountName("T");
        e.setEncryptedPassword("ep");
        when(vaultRepo.findByUserIdOrderByAccountNameAsc(1L)).thenReturn(Arrays.asList(e));
        when(enc.decrypt("ep")).thenReturn("weak");
        when(gen.strengthScore("weak")).thenReturn(1);
        when(gen.strengthLabel(1)).thenReturn("Weak");

        AuditReport r = auditService.generateReport(1L, 90);
        assertEquals(1, r.getWeakPasswords().size());
        assertEquals(90, r.getSecurityScore());
    }

    @Test
    void testReusedPassword() {
        VaultEntry e1 = new VaultEntry();
        e1.setId(1L);
        e1.setAccountName("A");
        e1.setEncryptedPassword("x");
        VaultEntry e2 = new VaultEntry();
        e2.setId(2L);
        e2.setAccountName("B");
        e2.setEncryptedPassword("x");
        when(vaultRepo.findByUserIdOrderByAccountNameAsc(1L)).thenReturn(Arrays.asList(e1, e2));
        when(enc.decrypt("x")).thenReturn("same");
        when(gen.strengthScore("same")).thenReturn(4);

        assertEquals(2, auditService.generateReport(1L, 90).getReusedPasswords().size());
    }
}