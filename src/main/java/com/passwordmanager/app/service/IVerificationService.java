package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.User;

public interface IVerificationService {
    String generateAndSendOtp(User user, String purpose);

    String sendRegistrationOtp(String email);

    String generateCode(User user, String purpose);

    boolean validateCode(User user, String code, String purpose);
}
