package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.SecurityQuestion;
import java.util.List;

public interface IPasswordRecoveryService {
    List<SecurityQuestion> getQuestions(String usernameOrEmail);

    boolean validateAnswers(Long userId, List<String> answers);

    void resetPassword(String usernameOrEmail, String newPassword);
}
