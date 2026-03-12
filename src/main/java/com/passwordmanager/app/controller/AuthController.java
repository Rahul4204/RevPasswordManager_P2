package com.passwordmanager.app.controller;

import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.dto.SecurityQuestionDTO;
import com.passwordmanager.app.entity.SecurityQuestion;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.service.IPasswordRecoveryService;
import com.passwordmanager.app.service.IUserService;
import com.passwordmanager.app.service.IVerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);

    private final IUserService userService;
    private final IPasswordRecoveryService recoveryService;
    private final IVerificationService verificationService;

    public AuthController(IUserService userService,
            IPasswordRecoveryService recoveryService,
            IVerificationService verificationService) {
        this.userService = userService;
        this.recoveryService = recoveryService;
        this.verificationService = verificationService;
    }

    @GetMapping("/")
    public String indexPage() {
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String expired,
            Model model) {
        if (error != null) {
            if ("dbDown".equals(error)) {
                model.addAttribute("loginError",
                        "System is temporarily unavailable due to a database issue. Please try again later.");
            } else {
                model.addAttribute("loginError", "Invalid username/email or password");
            }
        }

        if (logout != null)
            model.addAttribute("logoutMsg", "You have been logged out successfully");
        if (expired != null)
            model.addAttribute("loginError", "Your session has expired. Please login again");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        RegisterDTO dto = new RegisterDTO();
        dto.setSecurityQuestions(new ArrayList<>(List.of(
                new SecurityQuestionDTO(), new SecurityQuestionDTO(), new SecurityQuestionDTO())));
        model.addAttribute("registerDTO", dto);
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttrs,
            HttpSession session,
            Model model) {

        logger.info("Registration attempt for username: {}, email: {}", dto.getUsername(), dto.getEmail());

        if (result.hasErrors()) {
            logger.warn("Registration validation failed for user {}: {}", dto.getUsername(), result.getAllErrors());
            return "auth/register";
        }
        try {
            // Pre-validate to catch obvious errors before sending the OTP
            userService.preValidateRegistration(dto);
            logger.info("User {} successfully pre-validated. Sending OTP...", dto.getUsername());

            // Send email OTP for verification
            String otp = verificationService.sendRegistrationOtp(dto.getEmail());

            // Store registration DTO, OTP, and expiry in session
            session.setAttribute("pendingRegisterDTO", dto);
            session.setAttribute("pendingRegisterOtp", otp);
            session.setAttribute("pendingRegisterOtpExpiry", System.currentTimeMillis() + (10 * 60 * 1000)); // 10
                                                                                                             // minutes

            redirectAttrs.addFlashAttribute("successMsg",
                    "Success! Check your email for a verification code to complete registration.");
            return "redirect:/auth/verify-email";
        } catch (Exception e) {
            logger.error("Registration failed for user {}: {}", dto.getUsername(), e.getMessage(), e);
            model.addAttribute("errorMsg", e.getMessage());
            return "auth/register";
        }
    }

    // ===== Email Verification (after registration) =====

    @GetMapping("/auth/verify-email")
    public String verifyEmailPage(HttpSession session, Model model) {
        if (session.getAttribute("pendingRegisterDTO") == null)
            return "redirect:/login";
        model.addAttribute("purpose", "EMAIL_VERIFY");
        model.addAttribute("returnUrl", "/login?verified=true");
        return "auth/verify-email";
    }

    @PostMapping("/auth/verify-email")
    public String doVerifyEmail(@RequestParam String otp,
            HttpSession session,
            RedirectAttributes redirectAttrs) {
        RegisterDTO dto = (RegisterDTO) session.getAttribute("pendingRegisterDTO");
        String sessionOtp = (String) session.getAttribute("pendingRegisterOtp");
        Long otpExpiry = (Long) session.getAttribute("pendingRegisterOtpExpiry");

        if (dto == null || sessionOtp == null || otpExpiry == null)
            return "redirect:/login";

        if (System.currentTimeMillis() > otpExpiry) {
            redirectAttrs.addFlashAttribute("errorMsg", "Verification code expired. Please register again.");
            session.removeAttribute("pendingRegisterDTO");
            session.removeAttribute("pendingRegisterOtp");
            session.removeAttribute("pendingRegisterOtpExpiry");
            return "redirect:/register";
        }

        if (!sessionOtp.equals(otp)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Invalid verification code. Please try again.");
            return "redirect:/auth/verify-email";
        }

        try {
            // OTP is valid, proceed with actual registration
            userService.register(dto);

            session.removeAttribute("pendingRegisterDTO");
            session.removeAttribute("pendingRegisterOtp");
            session.removeAttribute("pendingRegisterOtpExpiry");

            redirectAttrs.addFlashAttribute("successMsg", "Email verified and account created! You can now login.");
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("Registration failed during verification for user {}: {}", dto.getUsername(), e.getMessage(),
                    e);
            redirectAttrs.addFlashAttribute("errorMsg", "Error creating account: " + e.getMessage());
            return "redirect:/register";
        }
    }

    // ===== Password Recovery =====

    @GetMapping("/recover")
    public String recoverPage() {
        return "auth/recover";
    }

    @PostMapping("/recover/questions")
    public String showQuestions(@RequestParam String usernameOrEmail,
            Model model,
            RedirectAttributes redirectAttrs) {
        try {
            List<SecurityQuestion> questions = recoveryService.getQuestions(usernameOrEmail);
            model.addAttribute("questions", questions);
            model.addAttribute("usernameOrEmail", usernameOrEmail);
            return "auth/recover-questions";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/recover";
        }
    }

    @PostMapping("/recover/verify")
    public String verifyAnswers(@RequestParam String usernameOrEmail,
            @RequestParam List<Long> questionIds,
            @RequestParam List<String> answers,
            Model model,
            RedirectAttributes redirectAttrs) {
        try {
            User user = userService.findByUsernameOrEmail(usernameOrEmail);
            boolean valid = recoveryService.validateAnswers(user.getId(), answers);
            if (!valid) {
                redirectAttrs.addFlashAttribute("errorMsg", "One or more answers are incorrect");
                return "redirect:/recover";
            }
            model.addAttribute("usernameOrEmail", usernameOrEmail);
            return "auth/recover-reset";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/recover";
        }
    }

    @PostMapping("/recover/reset")
    public String resetPassword(@RequestParam String usernameOrEmail,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttrs) {
        if (!newPassword.equals(confirmPassword)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Passwords do not match");
            return "redirect:/recover";
        }
        try {
            recoveryService.resetPassword(usernameOrEmail, newPassword);
            redirectAttrs.addFlashAttribute("successMsg", "Password reset successful. Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/recover";
        }
    }

    @GetMapping("/auth/2fa-login")
    public String show2faLogin(HttpSession session) {
        if (session.getAttribute("pending2faUserId") == null) {
            return "redirect:/login";
        }
        return "auth/2fa-login";
    }

    @PostMapping("/auth/2fa-login")
    public String process2faLogin(@RequestParam String otp, HttpSession session, RedirectAttributes redirectAttrs) {
        Long userId = (Long) session.getAttribute("pending2faUserId");
        if (userId == null) {
            return "redirect:/login";
        }

        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        boolean isValid = verificationService.validateCode(user, otp, "LOGIN_2FA");
        if (!isValid) {
            redirectAttrs.addFlashAttribute("errorMsg", "Invalid or expired verification code.");
            return "redirect:/auth/2fa-login";
        }

        // OTP valid - finalize login
        session.removeAttribute("pending2faUserId");

        org.springframework.security.core.userdetails.UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getMasterPasswordHash(),
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);

        return "redirect:/dashboard";
    }
}
