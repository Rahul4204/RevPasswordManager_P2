package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.SecurityQuestion;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.repository.IUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PasswordRecoveryService implements IPasswordRecoveryService {

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
        Optional<User> user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
        if (user.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        return questionRepository.findByUserId(user.get().getId());
    }

    @Override
    public boolean validateAnswers(Long userId, List<String> answers) {
        List<SecurityQuestion> questions = questionRepository.findByUserId(userId);
        if (questions.size() != answers.size())
            return false;

        for (int i = 0; i < questions.size(); i++) {
            if (!passwordEncoder.matches(answers.get(i).toLowerCase().trim(), questions.get(i).getAnswerHash())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void resetPassword(String usernameOrEmail, String newPassword) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setMasterPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
