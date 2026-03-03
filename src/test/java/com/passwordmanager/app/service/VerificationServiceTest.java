package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VerificationCode;
import com.passwordmanager.app.repository.IVerificationCodeRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    private VerificationService vs;

    @Mock
    private IVerificationCodeRepository repo;

    @Mock
    private IEmailService email;

    @BeforeEach
    void setUp() {
        vs = new VerificationService(repo, email);
        ReflectionTestUtils.setField(vs, "expiryMinutes", 10);
    }

    @Test
    void testSendOtp() {
        String code = vs.sendRegistrationOtp("t@t.com");

        assertNotNull(code);
        assertEquals(6, code.length());

        verify(email).sendOtp(eq("t@t.com"), eq(code), eq("REGISTRATION"));
    }

    @Test
    void testValidate_Success() {
        User u = new User();
        u.setId(1L);

        VerificationCode vc = VerificationCode.builder()
                .code("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .build();

        when(repo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "T"))
                .thenReturn(Optional.of(vc));

        boolean result = vs.validateCode(u, "123456", "T");

        assertTrue(result);
        assertTrue(vc.isUsed());
    }

    @Test
    void testValidate_Expired() {
        User u = new User();
        u.setId(1L);

        VerificationCode vc = VerificationCode.builder()
                .code("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .used(false)
                .build();

        when(repo.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "T"))
                .thenReturn(Optional.of(vc));

        boolean result = vs.validateCode(u, "123456", "T");

        assertFalse(result);
    }
}