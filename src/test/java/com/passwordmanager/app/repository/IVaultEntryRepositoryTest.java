package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VaultEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * JUnit 5 + Mockito unit tests for IVaultEntryRepository.
 * No database or Spring context is loaded — the repository is mocked.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IVaultEntryRepository Tests")
class IVaultEntryRepositoryTest {

    @Mock
    private IVaultEntryRepository vaultRepo;

    private User user;
    private VaultEntry entry1;
    private VaultEntry entry2;
    private VaultEntry entry3;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .username("vaultuser")
                .email("vaultuser@example.com")
                .masterPasswordHash("$2a$10$hash")
                .emailVerified(true)
                .totpEnabled(false)
                .accountLocked(false)
                .build();

        entry1 = VaultEntry.builder()
                .user(user).accountName("Amazon")
                .websiteUrl("https://amazon.com")
                .accountUsername("user@amazon.com")
                .encryptedPassword("enc1")
                .category(VaultEntry.Category.SHOPPING)
                .favorite(true).build();

        entry2 = VaultEntry.builder()
                .user(user).accountName("Gmail")
                .websiteUrl("https://mail.google.com")
                .accountUsername("user@gmail.com")
                .encryptedPassword("enc2")
                .category(VaultEntry.Category.EMAIL)
                .favorite(false).build();

        entry3 = VaultEntry.builder()
                .user(user).accountName("HDFC Bank")
                .websiteUrl("https://hdfcbank.com")
                .accountUsername("user123")
                .encryptedPassword("enc3")
                .category(VaultEntry.Category.BANKING)
                .favorite(false).build();
    }

    // ── findByUserIdOrderByAccountNameAsc ─────────────────────────────────────

    @Test
    @DisplayName("findByUserIdOrderByAccountNameAsc — returns sorted entries")
    void findByUserIdOrderByAccountNameAsc_returnsSorted() {
        when(vaultRepo.findByUserIdOrderByAccountNameAsc(1L))
                .thenReturn(List.of(entry1, entry2, entry3));

        List<VaultEntry> result = vaultRepo.findByUserIdOrderByAccountNameAsc(1L);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getAccountName()).isEqualTo("Amazon");
        verify(vaultRepo).findByUserIdOrderByAccountNameAsc(1L);
    }

    @Test
    @DisplayName("findByUserIdOrderByAccountNameAsc — returns empty for unknown user")
    void findByUserIdOrderByAccountNameAsc_unknownUser_empty() {
        when(vaultRepo.findByUserIdOrderByAccountNameAsc(9999L)).thenReturn(List.of());

        assertThat(vaultRepo.findByUserIdOrderByAccountNameAsc(9999L)).isEmpty();
        verify(vaultRepo).findByUserIdOrderByAccountNameAsc(9999L);
    }

    // ── findByUserIdAndFavoriteTrueOrderByAccountNameAsc ──────────────────────

    @Test
    @DisplayName("findFavorites — returns only favorites")
    void findFavorites_returnsOnlyFavorites() {
        when(vaultRepo.findByUserIdAndFavoriteTrueOrderByAccountNameAsc(1L))
                .thenReturn(List.of(entry1));

        List<VaultEntry> favorites = vaultRepo.findByUserIdAndFavoriteTrueOrderByAccountNameAsc(1L);

        assertThat(favorites).hasSize(1);
        assertThat(favorites.get(0).getAccountName()).isEqualTo("Amazon");
        assertThat(favorites.get(0).isFavorite()).isTrue();
        verify(vaultRepo).findByUserIdAndFavoriteTrueOrderByAccountNameAsc(1L);
    }

    @Test
    @DisplayName("findFavorites — returns empty when no favorites exist")
    void findFavorites_noFavorites_empty() {
        when(vaultRepo.findByUserIdAndFavoriteTrueOrderByAccountNameAsc(1L))
                .thenReturn(List.of());

        assertThat(vaultRepo.findByUserIdAndFavoriteTrueOrderByAccountNameAsc(1L)).isEmpty();
        verify(vaultRepo).findByUserIdAndFavoriteTrueOrderByAccountNameAsc(1L);
    }

    // ── findByUserIdAndCategory ───────────────────────────────────────────────

    @Test
    @DisplayName("findByUserIdAndCategory — returns entries matching category")
    void findByUserIdAndCategory_filtersByCategory() {
        when(vaultRepo.findByUserIdAndCategory(1L, VaultEntry.Category.EMAIL))
                .thenReturn(List.of(entry2));

        List<VaultEntry> result = vaultRepo.findByUserIdAndCategory(1L, VaultEntry.Category.EMAIL);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountName()).isEqualTo("Gmail");
        verify(vaultRepo).findByUserIdAndCategory(1L, VaultEntry.Category.EMAIL);
    }

    @Test
    @DisplayName("findByUserIdAndCategory — returns empty for unmatched category")
    void findByUserIdAndCategory_noMatch_empty() {
        when(vaultRepo.findByUserIdAndCategory(1L, VaultEntry.Category.WORK))
                .thenReturn(List.of());

        assertThat(vaultRepo.findByUserIdAndCategory(1L, VaultEntry.Category.WORK)).isEmpty();
        verify(vaultRepo).findByUserIdAndCategory(1L, VaultEntry.Category.WORK);
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search — matches by account name")
    void search_matchesByAccountName() {
        when(vaultRepo.search(1L, "amazon")).thenReturn(List.of(entry1));

        List<VaultEntry> result = vaultRepo.search(1L, "amazon");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountName()).isEqualTo("Amazon");
        verify(vaultRepo).search(1L, "amazon");
    }

    @Test
    @DisplayName("search — matches by website URL keyword")
    void search_matchesByWebsiteUrl() {
        when(vaultRepo.search(1L, "google")).thenReturn(List.of(entry2));

        List<VaultEntry> result = vaultRepo.search(1L, "google");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountName()).isEqualTo("Gmail");
        verify(vaultRepo).search(1L, "google");
    }

    @Test
    @DisplayName("search — matches by account username")
    void search_matchesByUsername() {
        when(vaultRepo.search(1L, "user123")).thenReturn(List.of(entry3));

        List<VaultEntry> result = vaultRepo.search(1L, "user123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountName()).isEqualTo("HDFC Bank");
        verify(vaultRepo).search(1L, "user123");
    }

    @Test
    @DisplayName("search — returns empty for unmatched query")
    void search_noMatch_empty() {
        when(vaultRepo.search(1L, "xyz")).thenReturn(List.of());

        assertThat(vaultRepo.search(1L, "xyz")).isEmpty();
        verify(vaultRepo).search(1L, "xyz");
    }

    // ── findByIdAndUserId ─────────────────────────────────────────────────────

    @Test
    @DisplayName("findByIdAndUserId — returns entry when owner matches")
    void findByIdAndUserId_found() {
        when(vaultRepo.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry1));

        Optional<VaultEntry> result = vaultRepo.findByIdAndUserId(1L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getAccountName()).isEqualTo("Amazon");
        verify(vaultRepo).findByIdAndUserId(1L, 1L);
    }

    @Test
    @DisplayName("findByIdAndUserId — returns empty when user does not own entry")
    void findByIdAndUserId_wrongUser_empty() {
        when(vaultRepo.findByIdAndUserId(1L, 9999L)).thenReturn(Optional.empty());

        assertThat(vaultRepo.findByIdAndUserId(1L, 9999L)).isEmpty();
        verify(vaultRepo).findByIdAndUserId(1L, 9999L);
    }

    // ── countByUserId ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("countByUserId — returns correct count")
    void countByUserId_correct() {
        when(vaultRepo.countByUserId(1L)).thenReturn(3L);

        assertThat(vaultRepo.countByUserId(1L)).isEqualTo(3L);
        verify(vaultRepo).countByUserId(1L);
    }

    @Test
    @DisplayName("countByUserId — returns 0 for unknown user")
    void countByUserId_unknownUser_zero() {
        when(vaultRepo.countByUserId(9999L)).thenReturn(0L);

        assertThat(vaultRepo.countByUserId(9999L)).isEqualTo(0L);
        verify(vaultRepo).countByUserId(9999L);
    }

    // ── findRecentByUserId ────────────────────────────────────────────────────

    @Test
    @DisplayName("findRecentByUserId — returns limited entries")
    void findRecentByUserId_limited() {
        PageRequest pageable = PageRequest.of(0, 2);
        when(vaultRepo.findRecentByUserId(1L, pageable)).thenReturn(List.of(entry1, entry2));

        List<VaultEntry> result = vaultRepo.findRecentByUserId(1L, pageable);

        assertThat(result).hasSize(2);
        verify(vaultRepo).findRecentByUserId(1L, pageable);
    }

    @Test
    @DisplayName("findRecentByUserId — returns empty for unknown user")
    void findRecentByUserId_unknownUser_empty() {
        PageRequest pageable = PageRequest.of(0, 5);
        when(vaultRepo.findRecentByUserId(9999L, pageable)).thenReturn(List.of());

        assertThat(vaultRepo.findRecentByUserId(9999L, pageable)).isEmpty();
        verify(vaultRepo).findRecentByUserId(9999L, pageable);
    }

    // ── save / delete ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("save — returns saved entry")
    void save_returnsSavedEntry() {
        when(vaultRepo.save(entry1)).thenReturn(entry1);

        VaultEntry saved = vaultRepo.save(entry1);

        assertThat(saved.getAccountName()).isEqualTo("Amazon");
        verify(vaultRepo).save(entry1);
    }

    @Test
    @DisplayName("deleteById — invokes deleteById on repository")
    void deleteById_invoked() {
        doNothing().when(vaultRepo).deleteById(1L);

        vaultRepo.deleteById(1L);

        verify(vaultRepo, times(1)).deleteById(1L);
    }
}
