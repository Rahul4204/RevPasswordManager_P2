package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.ChangePasswordDTO;
import com.passwordmanager.app.dto.ProfileUpdateDTO;
import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.entity.User;
import java.util.Optional;

public interface IUserService {
    void preValidateRegistration(RegisterDTO dto);

    User register(RegisterDTO dto);

    User findByUsernameOrEmail(String usernameOrEmail);

    boolean verifyMasterPassword(User user, String rawPassword);

    User updateProfile(Long userId, ProfileUpdateDTO dto);

    void confirmEmailChange(Long userId, String otp);

    void markEmailVerified(Long userId);

    void changeMasterPassword(Long userId, ChangePasswordDTO dto);

    void toggle2FA(Long userId, boolean enable);

    Optional<User> findById(Long id);

    void deleteAccount(Long userId, String masterPassword);

    User removeProfilePhoto(Long userId);
}
