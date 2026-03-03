package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for IUserRepository.
 * No database or Spring context is loaded — the repository is mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IUserRepository Tests")
class IUserRepositoryTest {

    @Mock
    private IUserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("testuser")
                .email("testuser@example.com")
                .fullName("Test User")
                .masterPasswordHash("$2a$10$hashedpassword")
                .emailVerified(true)
                .totpEnabled(false)
                .accountLocked(false)
                .build();
    }

    // ── findByUsername ────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsername — returns user when username exists")
    void findByUsername_found() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        Optional<User> result = userRepository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("testuser@example.com");
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    @DisplayName("findByUsername — returns empty when username does not exist")
    void findByUsername_notFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findByUsername("unknown");

        assertThat(result).isEmpty();
        verify(userRepository).findByUsername("unknown");
    }

    // ── findByEmail ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmail — returns user when email exists")
    void findByEmail_found() {
        when(userRepository.findByEmail("testuser@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userRepository.findByEmail("testuser@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        verify(userRepository).findByEmail("testuser@example.com");
    }

    @Test
    @DisplayName("findByEmail — returns empty when email does not exist")
    void findByEmail_notFound() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userRepository.findByEmail("nobody@example.com");

        assertThat(result).isEmpty();
        verify(userRepository).findByEmail("nobody@example.com");
    }

    // ── existsByUsername ──────────────────────────────────────────────────────

    @Test
    @DisplayName("existsByUsername — returns true when username exists")
    void existsByUsername_true() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThat(userRepository.existsByUsername("testuser")).isTrue();
        verify(userRepository).existsByUsername("testuser");
    }

    @Test
    @DisplayName("existsByUsername — returns false when username does not exist")
    void existsByUsername_false() {
        when(userRepository.existsByUsername("ghost")).thenReturn(false);

        assertThat(userRepository.existsByUsername("ghost")).isFalse();
        verify(userRepository).existsByUsername("ghost");
    }

    // ── existsByEmail ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("existsByEmail — returns true when email exists")
    void existsByEmail_true() {
        when(userRepository.existsByEmail("testuser@example.com")).thenReturn(true);

        assertThat(userRepository.existsByEmail("testuser@example.com")).isTrue();
        verify(userRepository).existsByEmail("testuser@example.com");
    }

    @Test
    @DisplayName("existsByEmail — returns false when email does not exist")
    void existsByEmail_false() {
        when(userRepository.existsByEmail("ghost@example.com")).thenReturn(false);

        assertThat(userRepository.existsByEmail("ghost@example.com")).isFalse();
        verify(userRepository).existsByEmail("ghost@example.com");
    }

    // ── findByUsernameOrEmail ─────────────────────────────────────────────────

    @Test
    @DisplayName("findByUsernameOrEmail — finds user by username")
    void findByUsernameOrEmail_byUsername() {
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        Optional<User> result = userRepository.findByUsernameOrEmail("testuser", "testuser");

        assertThat(result).isPresent();
        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    @DisplayName("findByUsernameOrEmail — finds user by email")
    void findByUsernameOrEmail_byEmail() {
        when(userRepository.findByUsernameOrEmail(
                "testuser@example.com", "testuser@example.com"))
                .thenReturn(Optional.of(user));

        Optional<User> result = userRepository.findByUsernameOrEmail(
                "testuser@example.com", "testuser@example.com");

        assertThat(result).isPresent();
        verify(userRepository).findByUsernameOrEmail(
                "testuser@example.com", "testuser@example.com");
    }

    @Test
    @DisplayName("findByUsernameOrEmail — returns empty when no match")
    void findByUsernameOrEmail_notFound() {
        when(userRepository.findByUsernameOrEmail("x", "x")).thenReturn(Optional.empty());

        assertThat(userRepository.findByUsernameOrEmail("x", "x")).isEmpty();
        verify(userRepository).findByUsernameOrEmail("x", "x");
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save — returns saved user")
    void save_returnsSavedUser() {
        when(userRepository.save(user)).thenReturn(user);

        User saved = userRepository.save(user);

        assertThat(saved).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        verify(userRepository).save(user);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll — returns list of users")
    void findAll_returnsList() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<User> users = userRepository.findAll();

        assertThat(users).hasSize(1);
        verify(userRepository).findAll();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — returns user when ID exists")
    void findById_found() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userRepository.findById(1L);

        assertThat(result).isPresent();
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("findById — returns empty when ID does not exist")
    void findById_notFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userRepository.findById(99L)).isEmpty();
        verify(userRepository).findById(99L);
    }

    // ── deleteById ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteById — invokes delete on repository")
    void deleteById_invoked() {
        doNothing().when(userRepository).deleteById(1L);

        userRepository.deleteById(1L);

        verify(userRepository, times(1)).deleteById(1L);
    }
}
