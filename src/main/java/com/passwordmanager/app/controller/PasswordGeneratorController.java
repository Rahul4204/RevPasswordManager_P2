package com.passwordmanager.app.controller;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.service.IPasswordGeneratorService;
import com.passwordmanager.app.service.IUserService;
import com.passwordmanager.app.service.IVaultService;
import com.passwordmanager.app.util.AuthUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/generator")
public class PasswordGeneratorController {

    private static final Logger logger = LogManager.getLogger(PasswordGeneratorController.class);

    private final IPasswordGeneratorService generatorService;
    private final IVaultService vaultService;
    private final IUserService userService;
    private final AuthUtil authUtil;

    public PasswordGeneratorController(IPasswordGeneratorService generatorService,
            IVaultService vaultService,
            IUserService userService,
            AuthUtil authUtil) {
        this.generatorService = generatorService;
        this.vaultService = vaultService;
        this.userService = userService;
        this.authUtil = authUtil;
    }

    @GetMapping
    public String generatorPage(Model model) {
        model.addAttribute("config", new PasswordGeneratorConfigDTO());
        model.addAttribute("user", authUtil.getCurrentUser());
        return "generator/generator";
    }

    @PostMapping
    public String generate(@ModelAttribute("config") PasswordGeneratorConfigDTO config, Model model) {
        List<String> passwords = generatorService.generate(config);
        List<com.passwordmanager.app.dto.PasswordResultDTO> results = passwords.stream()
                .map(pw -> {
                    int score = generatorService.strengthScore(pw);
                    return new com.passwordmanager.app.dto.PasswordResultDTO(
                            pw,
                            score,
                            generatorService.strengthLabel(score));
                })
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("config", config);
        model.addAttribute("results", results);
        model.addAttribute("user", authUtil.getCurrentUser());
        return "generator/generator";
    }

    @PostMapping("/save")
    public String saveToVault(@RequestParam String password,
            @RequestParam String accountName,
            @RequestParam String masterPassword,
            RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Incorrect master password");
            return "redirect:/generator";
        }
        VaultEntryDTO dto = new VaultEntryDTO();
        dto.setAccountName(accountName);
        dto.setPassword(password);
        dto.setCategory(VaultEntry.Category.OTHER);
        vaultService.addEntry(user, dto);
        redirectAttrs.addFlashAttribute("successMsg", "Password saved to vault!");
        return "redirect:/vault";
    }
}
