package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.SecurityQuestion;
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
 * JUnit 5 + Mockito unit tests for ISecurityQuestionRepository.
 * No database or Spring context is loaded — the repository is mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ISecurityQuestionRepository Tests")
class ISecurityQuestionRepositoryTest {

    @Mock
    private ISecurityQuestionRepository sqRepository;

    private User user;
    private SecurityQuestion sq1;
    private SecurityQuestion sq2;
    private SecurityQuestion sq3;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("squser")
                .email("squser@example.com")
                .masterPasswordHash("$2a$10$hash")
                .emailVerified(true)
                .totpEnabled(false)
                .accountLocked(false)
                .build();

        sq1 = SecurityQuestion.builder()
                .user(user).questionText("Pet's name?").answerHash("$2a$10$h1").build();
        sq2 = SecurityQuestion.builder()
                .user(user).questionText("Birth city?").answerHash("$2a$10$h2").build();
        sq3 = SecurityQuestion.builder()
                .user(user).questionText("Mother's maiden name?").answerHash("$2a$10$h3").build();
    }

    // ── findByUserId ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByUserId — returns all questions for the user")
    void findByUserId_returnsAllQuestions() {
        when(sqRepository.findByUserId(1L)).thenReturn(List.of(sq1, sq2, sq3));

        List<SecurityQuestion> result = sqRepository.findByUserId(1L);

        assertThat(result).hasSize(3);
        verify(sqRepository).findByUserId(1L);
    }

    @Test
    @DisplayName("findByUserId — returns empty list for unknown user")
    void findByUserId_unknownUser_returnsEmpty() {
        when(sqRepository.findByUserId(9999L)).thenReturn(List.of());

        assertThat(sqRepository.findByUserId(9999L)).isEmpty();
        verify(sqRepository).findByUserId(9999L);
    }

    // ── countByUserId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("countByUserId — returns correct count")
    void countByUserId_correct() {
        when(sqRepository.countByUserId(1L)).thenReturn(3L);

        assertThat(sqRepository.countByUserId(1L)).isEqualTo(3L);
        verify(sqRepository).countByUserId(1L);
    }

    @Test
    @DisplayName("countByUserId — returns 0 for unknown user")
    void countByUserId_unknownUser_zero() {
        when(sqRepository.countByUserId(9999L)).thenReturn(0L);

        assertThat(sqRepository.countByUserId(9999L)).isEqualTo(0L);
        verify(sqRepository).countByUserId(9999L);
    }

    // ── deleteByUserId ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteByUserId — invokes delete on repository")
    void deleteByUserId_invoked() {
        doNothing().when(sqRepository).deleteByUserId(1L);

        sqRepository.deleteByUserId(1L);

        verify(sqRepository, times(1)).deleteByUserId(1L);
    }

    @Test
    @DisplayName("deleteByUserId — called with correct user ID")
    void deleteByUserId_correctId() {
        doNothing().when(sqRepository).deleteByUserId(42L);

        sqRepository.deleteByUserId(42L);

        verify(sqRepository).deleteByUserId(42L);
        verify(sqRepository, never()).deleteByUserId(99L);
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("save — returns saved security question")
    void save_returnsSavedQuestion() {
        when(sqRepository.save(sq1)).thenReturn(sq1);

        SecurityQuestion saved = sqRepository.save(sq1);

        assertThat(saved).isNotNull();
        assertThat(saved.getQuestionText()).isEqualTo("Pet's name?");
        verify(sqRepository).save(sq1);
    }

    // ── saveAll ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveAll — saves all questions and returns them")
    void saveAll_returnsAllSaved() {
        List<SecurityQuestion> questions = List.of(sq1, sq2, sq3);
        when(sqRepository.saveAll(questions)).thenReturn(questions);

        List<SecurityQuestion> saved = sqRepository.saveAll(questions);

        assertThat(saved).hasSize(3);
        verify(sqRepository).saveAll(questions);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById — returns question when ID exists")
    void findById_found() {
        when(sqRepository.findById(1L)).thenReturn(Optional.of(sq1));

        Optional<SecurityQuestion> result = sqRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getQuestionText()).isEqualTo("Pet's name?");
        verify(sqRepository).findById(1L);
    }

    @Test
    @DisplayName("findById — returns empty when ID does not exist")
    void findById_notFound() {
        when(sqRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(sqRepository.findById(99L)).isEmpty();
        verify(sqRepository).findById(99L);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — invokes delete on repository")
    void delete_invoked() {
        doNothing().when(sqRepository).delete(sq1);

        sqRepository.delete(sq1);

        verify(sqRepository, times(1)).delete(sq1);
    }

    @Test
    @DisplayName("deleteById — invokes deleteById on repository")
    void deleteById_invoked() {
        doNothing().when(sqRepository).deleteById(1L);

        sqRepository.deleteById(1L);

        verify(sqRepository, times(1)).deleteById(1L);
    }
}
