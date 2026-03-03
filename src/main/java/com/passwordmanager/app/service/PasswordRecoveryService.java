package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.SecurityQuestion;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.repository.IUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Service
public class PasswordRecoveryService implements IPasswordRecoveryService {

    private static final Logger logger = LogManager.getLogger(PasswordRecoveryService.class);

    private final IUserRepository userRepository;
    private final ISecurityQuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordRecoveryService(IUserRepository userRepository, ISecurityQuestionRepository questionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<SecurityQuestion> getQuestions(String usernameOrEmail) {
        logger.info("Attempting to retrieve security questions for user: {}", usernameOrEmail);
        Optional<User> user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (user.isEmpty()) {
            logger.warn("User {} not found during security question retrieval", usernameOrEmail);
            throw new RuntimeException("User not found");
        }
        return questionRepository.findByUserId(user.get().getId());
    }

    @Override
    public boolean validateAnswers(Long userId, List<String> answers) {
        logger.info("Validating {} security answers for user ID: {}", answers.size(), userId);
        List<SecurityQuestion> questions = questionRepository.findByUserId(userId);
        if (questions.size() != answers.size()) {
            logger.warn("Answer count ({}) does not match question count ({}) for user ID: {}", answers.size(),
                    questions.size(), userId);
            return false;
        }

        for (int i = 0; i < questions.size(); i++) {
            if (!passwordEncoder.matches(answers.get(i).toLowerCase().trim(), questions.get(i).getAnswerHash())) {
                logger.warn("Incorrect security answer provided for question index {} for user ID: {}", i, userId);
                return false;
            }
        }
        logger.info("Successfully validated security answers for user ID: {}", userId);
        return true;
    }

    @Override
    public void resetPassword(String usernameOrEmail, String newPassword) {
        logger.info("Attempting to reset master password for user: {}", usernameOrEmail);
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    logger.error("User {} not found during password reset", usernameOrEmail);
                    return new RuntimeException("User not found");
                });
        user.setMasterPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Successfully reset master password for user: {}", usernameOrEmail);
    }
}
