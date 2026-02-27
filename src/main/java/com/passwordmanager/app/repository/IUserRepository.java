package com.passwordmanager.app.repository;

import com.passwordmanager.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

@Repository
public interface IUserRepository extends JpaRepository<User, Long> {

    @NonNull
    List<User> findAll();

    @NonNull
    Optional<User> findById(@NonNull Long id);

    @NonNull
    <S extends User> S save(@NonNull S entity);

    void deleteById(@NonNull Long id);

    void delete(@NonNull User entity);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);
}
