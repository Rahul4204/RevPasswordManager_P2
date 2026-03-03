package com.passwordmanager.app.controller;

import com.passwordmanager.app.dto.ChangePasswordDTO;
import com.passwordmanager.app.entity.SecurityQuestion;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.ISecurityQuestionRepository;
import com.passwordmanager.app.service.ISecurityAuditService;
import com.passwordmanager.app.dto.AuditReport;
import com.passwordmanager.app.service.IUserService;
import com.passwordmanager.app.service.IVerificationService;
import com.passwordmanager.app.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/security")
public class SecurityController {

    private static final Logger logger = LogManager.getLogger(SecurityController.class);

    @Value("${app.audit.old-password-days:90}")
    private int oldPasswordDays;

    private final IUserService userService;
    private final ISecurityAuditService auditService;
    private final IVerificationService verificationService;
    private final ISecurityQuestionRepository sqRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthUtil authUtil;

    public SecurityController(IUserService userService,
            ISecurityAuditService auditService,
            IVerificationService verificationService,
            ISecurityQuestionRepository sqRepository,
            PasswordEncoder passwordEncoder,
            AuthUtil authUtil) {
        this.userService = userService;
        this.auditService = auditService;
        this.verificationService = verificationService;
        this.sqRepository = sqRepository;
        this.passwordEncoder = passwordEncoder;
        this.authUtil = authUtil;
    }

    // ===== Security Audit =====
    @GetMapping("/audit")
    public String audit(Model model) {
        User user = authUtil.getCurrentUser();
        AuditReport report = auditService.generateReport(user.getId(), oldPasswordDays);
        model.addAttribute("report", report);
        model.addAttribute("user", user);
        return "security/audit";
    }

    // ===== Change Master Password =====
    @GetMapping("/change-password")
    public String changePasswordPage(Model model) {
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        model.addAttribute("user", authUtil.getCurrentUser());
        return "security/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttrs,
            Model model) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to change master password", user.getUsername());
        if (result.hasErrors()) {
            logger.warn("Validation failed for master password change for user {}", user.getUsername());
            model.addAttribute("user", user);
            return "security/change-password";
        }
        try {
            userService.changeMasterPassword(user.getId(), dto);
            logger.info("Master password changed successfully for user {}", user.getUsername());
            redirectAttrs.addFlashAttribute("successMsg", "Master password changed successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            logger.error("Error changing master password for user {}: {}", user.getUsername(), e.getMessage());
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("user", user);
            return "security/change-password";
        }
    }

    // ===== 2FA (Email OTP) =====

    @GetMapping("/2fa")
    public String twoFactorPage(Model model) {
        User user = authUtil.getCurrentUser();
        model.addAttribute("user", user);
        return "security/2fa-setup";
    }

    /** Enable 2FA: send OTP to email, redirect to verify page */
    @PostMapping("/2fa/enable")
    public String enable2FA(RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} requesting to enable 2FA", user.getUsername());
        // Generate and email the OTP so user can confirm
        verificationService.generateAndSendOtp(user, "2FA");
        redirectAttrs.addFlashAttribute("infoMsg",
                "A verification code has been sent to " + user.getEmail() + ". Enter it below to enable 2FA.");
        return "redirect:/security/2fa/verify";
    }

    @GetMapping("/2fa/verify")
    public String twoFaVerifyPage(Model model) {
        model.addAttribute("user", authUtil.getCurrentUser());
        return "security/2fa-verify";
    }

    @PostMapping("/2fa/verify")
    public String confirmEnable2FA(@RequestParam String otp, RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} verifying OTP to enable 2FA", user.getUsername());
        boolean valid = verificationService.validateCode(user, otp, "2FA");
        if (!valid) {
            logger.warn("Invalid or expired 2FA verification code for user {}", user.getUsername());
            redirectAttrs.addFlashAttribute("errorMsg", "Invalid or expired code. Please try again.");
            return "redirect:/security/2fa/verify";
        }
        userService.toggle2FA(user.getId(), true);
        logger.info("User {} successfully enabled 2FA", user.getUsername());
        redirectAttrs.addFlashAttribute("successMsg",
                "Two-factor authentication enabled! You will receive an email OTP on each login.");
        return "redirect:/security/2fa";
    }

    @PostMapping("/2fa/disable")
    public String disable2FA(@RequestParam String masterPassword, RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to disable 2FA", user.getUsername());
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            logger.warn("User {} failed master password verification while disabling 2FA", user.getUsername());
            redirectAttrs.addFlashAttribute("errorMsg", "Incorrect master password");
            return "redirect:/security/2fa";
        }
        userService.toggle2FA(user.getId(), false);
        logger.info("User {} successfully disabled 2FA", user.getUsername());
        redirectAttrs.addFlashAttribute("successMsg", "Two-factor authentication disabled.");
        return "redirect:/security/2fa";
    }

    // ===== Security Questions =====
    @GetMapping("/questions")
    public String questionsPage(Model model) {
        User user = authUtil.getCurrentUser();
        List<SecurityQuestion> questions = sqRepository.findByUserId(user.getId());
        model.addAttribute("questions", questions);
        model.addAttribute("user", user);
        return "security/questions";
    }

    @PostMapping("/questions/update")
    public String updateQuestions(@RequestParam String masterPassword,
            @RequestParam List<String> questionTexts,
            @RequestParam List<String> answers,
            RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to update security questions", user.getUsername());
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            logger.warn("User {} failed master password verification while updating security questions",
                    user.getUsername());
            redirectAttrs.addFlashAttribute("errorMsg", "Incorrect master password");
            return "redirect:/security/questions";
        }
        if (questionTexts.size() < 3) {
            logger.warn("User {} provided less than 3 security questions", user.getUsername());
            redirectAttrs.addFlashAttribute("errorMsg", "Minimum 3 security questions required");
            return "redirect:/security/questions";
        }

        sqRepository.deleteByUserId(user.getId());

        // Reload a fresh managed User entity after the delete so JPA doesn't
        // complain about a detached instance being passed to saveAll().
        User managedUser = userService.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SecurityQuestion> newQuestions = new ArrayList<>();
        for (int i = 0; i < questionTexts.size(); i++) {
            if (!questionTexts.get(i).isBlank() && !answers.get(i).isBlank()) {
                newQuestions.add(SecurityQuestion.builder()
                        .user(managedUser)
                        .questionText(questionTexts.get(i))
                        .answerHash(passwordEncoder.encode(answers.get(i).toLowerCase().trim()))
                        .build());
            }
        }
        sqRepository.saveAll(newQuestions);
        logger.info("User {} successfully updated their security questions", user.getUsername());
        redirectAttrs.addFlashAttribute("successMsg", "Security questions updated!");
        return "redirect:/security/questions";
    }
}
