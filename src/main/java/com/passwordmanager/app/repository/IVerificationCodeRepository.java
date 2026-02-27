package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IVerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    @NonNull
    List<VerificationCode> findAll();

    @NonNull
    Optional<VerificationCode> findById(@NonNull Long id);

    @NonNull
    <S extends VerificationCode> S save(@NonNull S entity);

    void deleteById(@NonNull Long id);

    void delete(@NonNull VerificationCode entity);

    Optional<VerificationCode> findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            Long userId, String purpose);

    @Modifying
    @Query("DELETE FROM VerificationCode vc WHERE vc.expiresAt < :now OR vc.used = true")
    void deleteExpiredAndUsed(@Param("now") LocalDateTime now);
}
