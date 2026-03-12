package com.passwordmanager.app.controller;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.entity.VaultEntry;
import com.passwordmanager.app.exception.ValidationException;
import com.passwordmanager.app.service.IUserService;
import com.passwordmanager.app.service.IVaultService;
import com.passwordmanager.app.util.AuthUtil;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.util.List;

@Controller
@RequestMapping("/vault")
public class VaultController {

    private static final Logger logger = LogManager.getLogger(VaultController.class);

    private final IVaultService vaultService;
    private final IUserService userService;
    private final AuthUtil authUtil;

    public VaultController(IVaultService vaultService, IUserService userService, AuthUtil authUtil) {
        this.vaultService = vaultService;
        this.userService = userService;
        this.authUtil = authUtil;
    }

    private static final List<String> PREDEFINED_CATEGORIES = List.of(
            "Banking", "Email", "Social Media", "Shopping", "Work", "Other");

    @GetMapping
    public String vaultList(@RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "name") String sort,
            Model model) {
        User user = authUtil.getCurrentUser();
        List<VaultEntryDTO> entries = vaultService.getAllEntries(user.getId(), search, category, sort);
        model.addAttribute("entries", entries);
        model.addAttribute("search", search);
        model.addAttribute("category", category != null ? category : "ALL");
        model.addAttribute("sort", sort);

        model.addAttribute("categories", PREDEFINED_CATEGORIES);
        model.addAttribute("user", user);
        return "vault/vault";
    }

    @GetMapping("/favorites")
    public String favorites(Model model) {
        User user = authUtil.getCurrentUser();
        model.addAttribute("entries", vaultService.getFavorites(user.getId()));
        model.addAttribute("user", user);
        return "vault/favorites";
    }

    @GetMapping("/{id}")
    public String viewEntry(@PathVariable Long id, Model model) {
        User user = authUtil.getCurrentUser();
        VaultEntryDTO entry = vaultService.getEntryMasked(user.getId(), id);
        model.addAttribute("entry", entry);
        model.addAttribute("revealed", false);
        model.addAttribute("user", user);
        return "vault/entry-detail";
    }

    @PostMapping("/{id}/reveal")
    public String revealPassword(@PathVariable Long id,
            @RequestParam String masterPassword,
            Model model,
            RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to reveal password for entry ID: {}", user.getUsername(), id);
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            logger.warn("User {} failed master password verification to reveal entry ID: {}", user.getUsername(), id);
            redirectAttrs.addFlashAttribute("errorMsg", "Incorrect master password");
            return "redirect:/vault/" + id;
        }
        VaultEntryDTO entry = vaultService.getEntryWithDecryptedPassword(user.getId(), id);
        logger.info("Password revealed successfully for User {} and entry ID: {}", user.getUsername(), id);
        model.addAttribute("entry", entry);
        model.addAttribute("revealed", true);
        model.addAttribute("user", user);
        return "vault/entry-detail";
    }

    @GetMapping("/add")
    public String addEntryPage(Model model) {
        User user = authUtil.getCurrentUser();
        model.addAttribute("entryDTO", new VaultEntryDTO());
        model.addAttribute("categories", PREDEFINED_CATEGORIES);
        model.addAttribute("isEdit", false);
        model.addAttribute("user", user);
        return "vault/add-edit-entry";
    }

    @PostMapping("/add")
    public String addEntry(@Valid @ModelAttribute("entryDTO") VaultEntryDTO dto,
            BindingResult result,
            @RequestParam String masterPassword,
            Model model,
            RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to add new vault entry for account {}", user.getUsername(),
                dto.getAccountName());
        if (result.hasErrors()) {
            logger.warn("Validation failed adding vault entry for user {}", user.getUsername());
            model.addAttribute("entryDTO", dto);
            model.addAttribute("categories", PREDEFINED_CATEGORIES);
            model.addAttribute("isEdit", false);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            logger.warn("User {} failed master password verification to add vault entry", user.getUsername());
            model.addAttribute("entryDTO", dto);
            model.addAttribute("errorMsg", "Incorrect master password");
            model.addAttribute("categories", PREDEFINED_CATEGORIES);
            model.addAttribute("isEdit", false);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        vaultService.addEntry(user, dto);
        logger.info("Vault entry added successfully for user {}, account: {}", user.getUsername(),
                dto.getAccountName());
        redirectAttrs.addFlashAttribute("successMsg", "Password entry added successfully!");
        return "redirect:/vault";
    }

    @GetMapping("/{id}/edit")
    public String editEntryPage(@PathVariable Long id, Model model) {
        User user = authUtil.getCurrentUser();
        VaultEntryDTO entry = vaultService.getEntryWithDecryptedPassword(user.getId(), id);
        model.addAttribute("entryDTO", entry);
        model.addAttribute("categories", PREDEFINED_CATEGORIES);
        model.addAttribute("isEdit", true);
        model.addAttribute("user", user);
        return "vault/add-edit-entry";
    }

    @PostMapping("/{id}/edit")
    public String editEntry(@PathVariable Long id,
            @Valid @ModelAttribute("entryDTO") VaultEntryDTO dto,
            BindingResult result,
            @RequestParam String masterPassword,
            Model model,
            RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to edit vault entry ID: {}", user.getUsername(), id);
        if (result.hasErrors()) {
            logger.warn("Validation failed editing vault entry ID {} for user {}", id, user.getUsername());
            model.addAttribute("entryDTO", dto);
            model.addAttribute("categories", PREDEFINED_CATEGORIES);
            model.addAttribute("isEdit", true);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            logger.warn("User {} failed master password verification editing vault entry ID: {}", user.getUsername(),
                    id);
            model.addAttribute("entryDTO", dto);
            model.addAttribute("errorMsg", "Incorrect master password");
            model.addAttribute("categories", PREDEFINED_CATEGORIES);
            model.addAttribute("isEdit", true);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        vaultService.updateEntry(user.getId(), id, dto);
        logger.info("Vault entry ID {} updated successfully for user {}", id, user.getUsername());
        redirectAttrs.addFlashAttribute("successMsg", "Entry updated!");
        return "redirect:/vault/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteEntry(@PathVariable Long id,
            @RequestParam String masterPassword,
            RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} attempting to delete vault entry ID: {}", user.getUsername(), id);
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            logger.warn("User {} failed master password verification deleting vault entry ID: {}", user.getUsername(),
                    id);
            redirectAttrs.addFlashAttribute("errorMsg", "Incorrect master password");
            return "redirect:/vault/" + id;
        }
        vaultService.deleteEntry(user.getId(), id);
        logger.info("Vault entry ID {} deleted successfully for user {}", id, user.getUsername());
        redirectAttrs.addFlashAttribute("successMsg", "Entry deleted");
        return "redirect:/vault";
    }

    @PostMapping("/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "/vault") String returnUrl) {
        User user = authUtil.getCurrentUser();
        logger.info("User {} toggling favorite for vault entry ID: {}", user.getUsername(), id);
        vaultService.toggleFavorite(user.getId(), id);
        return "redirect:" + returnUrl;
    }
}
