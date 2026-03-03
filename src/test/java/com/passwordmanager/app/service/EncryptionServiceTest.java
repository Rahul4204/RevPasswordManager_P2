package com.passwordmanager.app.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        ReflectionTestUtils.setField(encryptionService, "secret", "test-secret-key-1234567890123456");
    }

    @Test
    void testEncryptDecrypt() {
        String plain = "MySecretPassword123";
        String encrypted = encryptionService.encrypt(plain);
        assertNotNull(encrypted);
        assertNotEquals(plain, encrypted);
        assertEquals(plain, encryptionService.decrypt(encrypted));
    }

    @Test
    void testDecryptInvalidText() {
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt("invalid-base64-text"));
    }
}