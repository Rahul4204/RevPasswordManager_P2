package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.exception.ValidationException;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.repository.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private UserService userService;

    @Mock
    private IUserRepository userRepository;
    @Mock
    private ISecurityQuestionRepository sqRepo;
    @Mock
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, sqRepo, encoder);
    }

    @Test
    void testVerifyMasterPassword() {
        User u = User.builder().masterPasswordHash("h").build();
        when(encoder.matches("raw", "h")).thenReturn(true);
        assertTrue(userService.verifyMasterPassword(u, "raw"));
    }

    @Test
    void testRegisterPasswordsMismatch() {
        RegisterDTO d = new RegisterDTO();
        d.setMasterPassword("a");
        d.setConfirmPassword("b");
        assertThrows(ValidationException.class, () -> userService.register(d));
    }

    @Test
    void testRegisterDuplicateUsername() {
        RegisterDTO d = new RegisterDTO();
        d.setUsername("x");
        d.setMasterPassword("p");
        d.setConfirmPassword("p");
        when(userRepository.existsByUsername("x")).thenReturn(true);
        assertThrows(ValidationException.class, () -> userService.register(d));
    }

    @Test
    void testFindByUsernameOrEmail() {
        User u = new User();
        u.setUsername("t");
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.of(u));
        assertEquals("t", userService.findByUsernameOrEmail("t").getUsername());
    }
}