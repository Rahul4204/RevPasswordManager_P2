package com.passwordmanager.app.controller;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.service.ISecurityAuditService;
import com.passwordmanager.app.dto.AuditReport;
import com.passwordmanager.app.service.IVaultService;
import com.passwordmanager.app.util.AuthUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class DashboardController {

    private static final Logger logger = LogManager.getLogger(DashboardController.class);

    @Value("${app.audit.old-password-days:90}")
    private int oldPasswordDays;

    private final IVaultService vaultService;
    private final ISecurityAuditService auditService;
    private final AuthUtil authUtil;

    public DashboardController(IVaultService vaultService, ISecurityAuditService auditService, AuthUtil authUtil) {
        this.vaultService = vaultService;
        this.auditService = auditService;
        this.authUtil = authUtil;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = authUtil.getCurrentUser();
        if (user == null)
            return "redirect:/login";

        long total = vaultService.countByUser(user.getId());
        AuditReport report = auditService.generateReport(user.getId(), oldPasswordDays);
        List<VaultEntryDTO> recent = vaultService.getRecentEntries(user.getId(), 5);

        model.addAttribute("user", user);
        model.addAttribute("totalPasswords", total);
        model.addAttribute("weakCount", report.getWeakPasswords().size());
        model.addAttribute("reusedCount", report.getReusedPasswords().size());
        model.addAttribute("oldCount", report.getOldPasswords().size());
        model.addAttribute("recentEntries", recent);

        logger.debug("Dashboard loaded for user: {}", user.getUsername());
        return "dashboard/dashboard";
    }
}
