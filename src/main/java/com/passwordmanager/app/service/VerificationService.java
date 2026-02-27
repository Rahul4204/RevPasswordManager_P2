package com.passwordmanager.app.service;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VerificationCode;
import com.passwordmanager.app.repository.IVerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class VerificationService implements IVerificationService {

    private final IVerificationCodeRepository codeRepo;
    private final IEmailService emailService;

    @Value("${app.verification.expiry-minutes:10}")
    private int expiryMinutes;

    public VerificationService(IVerificationCodeRepository codeRepo, IEmailService emailService) {
        this.codeRepo = codeRepo;
        this.emailService = emailService;
    }

    @Override
    public String generateAndSendOtp(User user, String purpose) {
        String code = generateCode(user, purpose);
        emailService.sendOtp(user.getEmail(), code, purpose);
        return code;
    }

    @Override
    public String sendRegistrationOtp(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        emailService.sendOtp(email, code, "REGISTRATION");
        return code;
    }

    @Override
    public String generateCode(User user, String purpose) {
        String rawCode = String.format("%06d", new SecureRandom().nextInt(999999));
        VerificationCode code = VerificationCode.builder()
                .user(user)
                .code(rawCode)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .build();
        codeRepo.save(code);
        return rawCode;
    }

    @Override
    public boolean validateCode(User user, String code, String purpose) {
        Optional<VerificationCode> optCode = codeRepo
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(user.getId(), purpose);
        if (optCode.isPresent()) {
            VerificationCode vc = optCode.get();
            if (vc.isValid() && vc.getCode().equals(code)) {
                vc.setUsed(true);
                codeRepo.save(vc);
                return true;
            }
        }
        return false;
    }
}
