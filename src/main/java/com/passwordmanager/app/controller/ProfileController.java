package com.passwordmanager.app.controller;

import com.passwordmanager.app.dto.ProfileUpdateDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.exception.ValidationException;
import com.passwordmanager.app.service.IUserService;
import com.passwordmanager.app.service.IVerificationService;
import com.passwordmanager.app.util.AuthUtil;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger logger = LogManager.getLogger(ProfileController.class);

    private final IUserService userService;
    private final IVerificationService verificationService;
    private final AuthUtil authUtil;

    public ProfileController(IUserService userService,
            IVerificationService verificationService,
            AuthUtil authUtil) {
        this.userService = userService;
        this.verificationService = verificationService;
        this.authUtil = authUtil;
    }

    @GetMapping
    public String profilePage(Model model) {
        User user = authUtil.getCurrentUser();
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        model.addAttribute("profileDTO", dto);
        model.addAttribute("user", user);
        return "profile/profile";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("profileDTO") ProfileUpdateDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttrs,
            Model model) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to update profile", user.getUsername());
        if (result.hasErrors()) {
            logger.warn("Profile update validation errors for user {}: {}", user.getUsername(), result.getAllErrors());
            model.addAttribute("user", user);
            return "profile/profile";
        }
        try {
            User updated = userService.updateProfile(user.getId(), dto);
            logger.info("User {} profile updated successfully", user.getUsername());
            // If a new email was provided, OTP was sent — redirect to confirm page
            if (updated.getPendingEmail() != null) {
                redirectAttrs.addFlashAttribute("infoMsg",
                        "A verification code has been sent to " + updated.getPendingEmail()
                                + ". Enter it to confirm your new email.");
                return "redirect:/profile/verify-email";
            }
            redirectAttrs.addFlashAttribute("successMsg", "Profile updated successfully!");
        } catch (ValidationException e) {
            logger.warn("Validation error during profile update for user {}: {}", user.getUsername(), e.getMessage());
            model.addAttribute("errorMsg", e.getMessage());
            model.addAttribute("user", user);
            return "profile/profile";
        }
        return "redirect:/profile";
    }

    @PostMapping("/remove-photo")
    public String removeProfilePhoto(RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        if (user == null) {
            return "redirect:/login";
        }
        logger.info("User {} removing profile photo", user.getUsername());
        userService.removeProfilePhoto(user.getId());
        redirectAttrs.addFlashAttribute("successMsg", "Profile photo removed successfully!");
        return "redirect:/profile";
    }

    // ===== Email change OTP verification =====

    @GetMapping("/verify-email")
    public String verifyEmailPage(Model model) {
        User user = authUtil.getCurrentUser();
        if (user == null || user.getPendingEmail() == null)
            return "redirect:/profile";
        model.addAttribute("user", user);
        model.addAttribute("pendingEmail", user.getPendingEmail());
        return "profile/profile-verify-email";
    }

    @PostMapping("/verify-email")
    public String doVerifyEmail(@RequestParam String otp, RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        if (user == null)
            return "redirect:/login";

        logger.info("User {} verifying new email address with OTP", user.getUsername());
        try {
            userService.confirmEmailChange(user.getId(), otp);
            redirectAttrs.addFlashAttribute("successMsg",
                    "Email address updated and verified successfully!");
        } catch (ValidationException e) {
            logger.warn("Email verification failed for user {}: {}", user.getUsername(), e.getMessage());
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/profile/verify-email";
        }
        return "redirect:/profile";
    }

    // ===== Account Deletion =====

    @GetMapping("/delete")
    public String deleteAccountPage(Model model) {
        User user = authUtil.getCurrentUser();
        if (user == null)
            return "redirect:/login";
        model.addAttribute("user", user);
        return "profile/profile-delete";
    }

    @PostMapping("/delete")
    public String doDeleteAccount(@RequestParam String masterPassword, RedirectAttributes redirectAttrs,
            jakarta.servlet.http.HttpSession session) {
        User user = authUtil.getCurrentUser();
        if (user == null)
            return "redirect:/login";

        logger.info("User {} requesting account deletion", user.getUsername());

        try {
            userService.deleteAccount(user.getId(), masterPassword);
            // Invalidate session
            session.invalidate();
            // Clear security context
            org.springframework.security.core.context.SecurityContextHolder.clearContext();

            logger.info("User {} account successfully deleted", user.getUsername());
            return "redirect:/login?logout=true";
        } catch (Exception e) {
            logger.error("Account deletion failed for user {}: {}", user.getUsername(), e.getMessage(), e);
            redirectAttrs.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/profile/delete";
        }
    }
}
