package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.ChangePasswordDTO;
import com.passwordmanager.app.dto.ProfileUpdateDTO;
import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.exception.InvalidCredentialsException;
import com.passwordmanager.app.exception.ResourceNotFoundException;
import com.passwordmanager.app.exception.ValidationException;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.repository.IUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import com.passwordmanager.app.entity.SecurityQuestion;
import java.security.SecureRandom;
import java.util.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class UserService implements IUserService {

    private static final Logger logger = LogManager.getLogger(UserService.class);

    private final IUserRepository userRepository;
    private final ISecurityQuestionRepository sqRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(IUserRepository userRepository, ISecurityQuestionRepository sqRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sqRepository = sqRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void preValidateRegistration(RegisterDTO dto) {
        if (!dto.getMasterPassword().equals(dto.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new ValidationException("Username already exists");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ValidationException("Email already exists");
        }
        if (dto.getSecurityQuestions() == null || dto.getSecurityQuestions().size() < 3) {
            throw new ValidationException("At least 3 security questions are required");
        }
    }

    @Override
    public User register(RegisterDTO dto) {
        logger.info("Starting registration process for user: {}", dto.getUsername());
        preValidateRegistration(dto);
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .fullName(dto.getFullName())
                .masterPasswordHash(passwordEncoder.encode(dto.getMasterPassword()))
                .emailVerified(true)
                .totpEnabled(false)
                .accountLocked(false)
                .build();
        User saved = userRepository.save(user);

        if (dto.getSecurityQuestions() != null) {
            List<SecurityQuestion> questions = dto.getSecurityQuestions().stream().map(qDto -> {
                SecurityQuestion sq = new SecurityQuestion();
                sq.setUser(saved);
                sq.setQuestionText(qDto.getQuestionText());
                sq.setAnswerHash(passwordEncoder.encode(qDto.getAnswer().toLowerCase().trim()));
                return sq;
            }).collect(Collectors.toList());
            sqRepository.saveAll(questions);
        }

        logger.info("Successfully registered user: {}", saved.getUsername());
        return saved;
    }

    @Override
    public User findByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail).orElse(null);
    }

    @Override
    public boolean verifyMasterPassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getMasterPasswordHash());
    }

    @Override
    public User updateProfile(Long userId, ProfileUpdateDTO dto) {
        logger.info("Attempting to update profile for user ID: {}", userId);
        User user = userRepository.findById(userId).orElseThrow();
        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());

        if (dto.getEmail() != null && !dto.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new ValidationException("Email already in use");
            }
            user.setPendingEmail(dto.getEmail());
            logger.info("User ID: {} requested email change to {}", userId, dto.getEmail());
        }
        User saved = userRepository.save(user);
        logger.info("Successfully updated profile for user ID: {}", userId);
        return saved;
    }

    @Override
    public void confirmEmailChange(Long userId, String otp) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getPendingEmail() != null) {
            logger.info("User ID: {} confirmed new email address: {}", userId, user.getPendingEmail());
            user.setEmail(user.getPendingEmail());
            user.setPendingEmail(null);
            user.setEmailVerified(true);
            userRepository.save(user);
        }
    }

    @Override
    public void markEmailVerified(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    @Override
    public void changeMasterPassword(Long userId, ChangePasswordDTO dto) {
        logger.info("Attempting to change master password for user ID: {}", userId);
        User user = userRepository.findById(userId).orElseThrow();
        if (!verifyMasterPassword(user, dto.getCurrentPassword())) {
            logger.warn("Incorrect current password provided during master password change for user ID: {}", userId);
            throw new InvalidCredentialsException("Incorrect old password");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
            throw new ValidationException("Passwords do not match");
        }
        user.setMasterPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
        logger.info("Successfully changed master password for user ID: {}", userId);
    }

    @Override
    public void toggle2FA(Long userId, boolean enable) {
        logger.info("Toggling 2FA for user ID: {} to {}", userId, enable);
        User user = userRepository.findById(userId).orElseThrow();
        user.setTotpEnabled(enable);
        if (enable) {
            byte[] bytes = new byte[20];
            new SecureRandom().nextBytes(bytes);
            user.setTotpSecret(Base64.getEncoder().encodeToString(bytes).replace("=", ""));
        } else {
            user.setTotpSecret(null);
        }
        userRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public void deleteAccount(Long userId, String masterPassword) {
        logger.info("Attempting to delete account for user ID: {}", userId);
        User user = userRepository.findById(userId).orElseThrow();
        if (!verifyMasterPassword(user, masterPassword)) {
            logger.warn("Incorrect master password provided during account deletion for user ID: {}", userId);
            throw new RuntimeException("Incorrect master password");
        }
        userRepository.delete(user);
        logger.info("Successfully deleted account for user ID: {}", userId);
    }

    @Override
    public User removeProfilePhoto(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setProfilePhotoUrl(null);
        return userRepository.save(user);
    }
}
