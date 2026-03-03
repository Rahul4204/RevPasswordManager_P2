package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.SecurityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ISecurityQuestionRepository extends JpaRepository<SecurityQuestion, Long> {

    @NonNull
    List<SecurityQuestion> findAll();

    @NonNull
    Optional<SecurityQuestion> findById(@NonNull Long id);

    @NonNull
    <S extends SecurityQuestion> S save(@NonNull S entity);

    void deleteById(@NonNull Long id);

    void delete(@NonNull SecurityQuestion entity);

    List<SecurityQuestion> findByUserId(Long userId);

    long countByUserId(Long userId);

    @Transactional
    @Modifying
    void deleteByUserId(Long userId);
}
