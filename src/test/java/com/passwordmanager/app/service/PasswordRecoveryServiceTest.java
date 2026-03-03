package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.SecurityQuestion;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    private PasswordRecoveryService svc;

    @Mock
    private IUserRepository ur;
    @Mock
    private ISecurityQuestionRepository qr;
    @Mock
    private PasswordEncoder enc;

    @BeforeEach
    void setUp() {
        svc = new PasswordRecoveryService(ur, qr, enc);
    }

    @Test
    void testGetQuestions() {
        User u = new User();
        u.setId(1L);
        when(ur.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(u));
        when(qr.findByUserId(1L)).thenReturn(Arrays.asList(new SecurityQuestion()));
        assertEquals(1, svc.getQuestions("t").size());
    }

    @Test
    void testValidateAnswers() {
        SecurityQuestion q = new SecurityQuestion();
        q.setAnswerHash("h");
        when(qr.findByUserId(1L)).thenReturn(Arrays.asList(q));
        when(enc.matches(anyString(), eq("h"))).thenReturn(true);
        assertTrue(svc.validateAnswers(1L, Arrays.asList("a")));
    }

    @Test
    void testResetPassword() {
        User u = new User();
        when(ur.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(u));
        when(enc.encode("newPass")).thenReturn("hash");
        svc.resetPassword("t", "newPass");
        assertEquals("hash", u.getMasterPasswordHash());
        verify(ur).save(u);
    }
}