package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VerificationCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for IVerificationCodeRepository.
 * No database or Spring context is loaded — the repository is mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IVerificationCodeRepository Tests")
class IVerificationCodeRepositoryTest {

    @Mock
    private IVerificationCodeRepository vcRepository;

    private User user;
    private VerificationCode activeCode;
    private VerificationCode expiredCode;
    private VerificationCode usedCode;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("vcuser")
                .email("vcuser@example.com")
                .masterPasswordHash("$2a$10$hash")
                .emailVerified(true)
                .totpEnabled(false)
                .accountLocked(false)
                .build();

        activeCode = VerificationCode.builder()
                .user(user)
                .code("123456")
                .purpose("EMAIL_VERIFY")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        expiredCode = VerificationCode.builder()
                .user(user)
                .code("000000")
                .purpose("EMAIL_VERIFY")
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // already expired
                .used(false)
                .build();

        usedCode = VerificationCode.builder()
                .user(user)
                .code("999999")
                .purpose("LOGIN_2FA")
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true) // already consumed
                .build();
    }

    // ── findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc ─────────────

    @Test
    @DisplayName("findTop — returns active unused code for purpose")
    void findTop_returnsActiveCode() {
        when(vcRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                1L, "EMAIL_VERIFY"))
                .thenReturn(Optional.of(activeCode));

        Optional<VerificationCode> result = vcRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "EMAIL_VERIFY");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("123456");
        assertThat(result.get().isUsed()).isFalse();
        verify(vcRepository)
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "EMAIL_VERIFY");
    }

    @Test
    @DisplayName("findTop — returns empty when all codes for purpose are used")
    void findTop_allUsed_returnsEmpty() {
        when(vcRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                1L, "LOGIN_2FA"))
                .thenReturn(Optional.empty());

        Optional<VerificationCode> result = vcRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "LOGIN_2FA");

        assertThat(result).isEmpty();
        verify(vcRepository)
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "LOGIN_2FA");
    }

    @Test
    @DisplayName("findTop — returns empty for unknown purpose")
    void findTop_unknownPurpose_returnsEmpty() {
        when(vcRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                1L, "UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThat(vcRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "UNKNOWN"))
                .isEmpty();
        verify(vcRepository)
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(1L, "UNKNOWN");
    }

    @Test
    @DisplayName("findTop — returns empty for unknown user")
    void findTop_unknownUser_returnsEmpty() {
        when(vcRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                9999L, "EMAIL_VERIFY"))
                .thenReturn(Optional.empty());

        assertThat(vcRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(9999L, "EMAIL_VERIFY"))
                .isEmpty();
        verify(vcRepository)
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(9999L, "EMAIL_VERIFY");
    }

    // ── deleteExpiredAndUsed ──────────────────────────────────────────────────

    @Test
    @DisplayName("deleteExpiredAndUsed — invokes delete with correct timestamp")
    void deleteExpiredAndUsed_invoked() {
        LocalDateTime now = LocalDateTime.now();
        doNothing().when(vcRepository).deleteExpiredAndUsed(any(LocalDateTime.class));

        vcRepository.deleteExpiredAndUsed(now);

        verify(vcRepository, times(1)).deleteExpiredAndUsed(now);
    }

    // ── VerificationCode entity helper methods ────────────────────────────────

    @Test
    @DisplayName("VerificationCode.isValid — true for active unused code")
    void isValid_activeCode_true() {
        assertThat(activeCode.isValid()).isTrue();
    }

    @Test
    @DisplayName("VerificationCode.isValid — false for expired code")
    void isValid_expiredCode_false() {
        assertThat(expiredCode.isValid()).isFalse();
    }

    @Test
    @DisplayName("VerificationCode.isValid — false for used code")
    void isValid_usedCode_false() {
        assertThat(usedCode.isValid()).isFalse();
    }

    @Test
    @DisplayName("VerificationCode.isExpired — true when past expiry")
    void isExpired_pastExpiry_true() {
        assertThat(expiredCode.isExpired()).isTrue();
    }

    @Test
    @DisplayName("VerificationCode.isExpired — false when expiry is in future")
    void isExpired_futureExpiry_false() {
        assertThat(activeCode.isExpired()).isFalse();
    }

    // ── save / findById / deleteById / findAll ────────────────────────────────

    @Test
    @DisplayName("save — returns saved verification code")
    void save_returnsSavedCode() {
        when(vcRepository.save(activeCode)).thenReturn(activeCode);

        VerificationCode saved = vcRepository.save(activeCode);

        assertThat(saved.getCode()).isEqualTo("123456");
        assertThat(saved.getPurpose()).isEqualTo("EMAIL_VERIFY");
        verify(vcRepository).save(activeCode);
    }

    @Test
    @DisplayName("findById — returns code when ID exists")
    void findById_found() {
        when(vcRepository.findById(1L)).thenReturn(Optional.of(activeCode));

        Optional<VerificationCode> result = vcRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("123456");
        verify(vcRepository).findById(1L);
    }

    @Test
    @DisplayName("findById — returns empty when ID does not exist")
    void findById_notFound() {
        when(vcRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(vcRepository.findById(99L)).isEmpty();
        verify(vcRepository).findById(99L);
    }

    @Test
    @DisplayName("findAll — returns all codes")
    void findAll_returnsAll() {
        when(vcRepository.findAll()).thenReturn(List.of(activeCode, expiredCode, usedCode));

        List<VerificationCode> all = vcRepository.findAll();

        assertThat(all).hasSize(3);
        verify(vcRepository).findAll();
    }

    @Test
    @DisplayName("deleteById — invokes deleteById on repository")
    void deleteById_invoked() {
        doNothing().when(vcRepository).deleteById(1L);

        vcRepository.deleteById(1L);

        verify(vcRepository, times(1)).deleteById(1L);
    }
}
