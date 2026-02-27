package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.VaultEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

@Repository
public interface IVaultEntryRepository extends JpaRepository<VaultEntry, Long> {

        @NonNull
        List<VaultEntry> findAll();

        @NonNull
        Optional<VaultEntry> findById(@NonNull Long id);

        @NonNull
        <S extends VaultEntry> S save(@NonNull S entity);

        void deleteById(@NonNull Long id);

        void delete(@NonNull VaultEntry entity);

        List<VaultEntry> findByUserIdOrderByAccountNameAsc(Long userId);

        List<VaultEntry> findByUserIdAndFavoriteTrueOrderByAccountNameAsc(Long userId);

        List<VaultEntry> findByUserIdAndCategory(Long userId, VaultEntry.Category category);

        @Query("SELECT v FROM VaultEntry v WHERE v.user.id = :userId AND " +
                        "(LOWER(v.accountName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                        " LOWER(v.websiteUrl) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                        " LOWER(v.accountUsername) LIKE LOWER(CONCAT('%', :q, '%')))")
        List<VaultEntry> search(@Param("userId") Long userId, @Param("q") String query);

        Optional<VaultEntry> findByIdAndUserId(Long id, Long userId);

        long countByUserId(Long userId);

        @Query("SELECT v FROM VaultEntry v WHERE v.user.id = :userId ORDER BY v.createdAt DESC")
        List<VaultEntry> findRecentByUserId(@Param("userId") Long userId,
                        org.springframework.data.domain.Pageable pageable);
}
