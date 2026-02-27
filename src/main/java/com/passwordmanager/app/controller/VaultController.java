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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping
    public String vaultList(@RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "list") String view,
            Model model) {
        User user = authUtil.getCurrentUser();
        List<VaultEntryDTO> entries = vaultService.getAllEntries(user.getId(), search, category, sort);
        model.addAttribute("entries", entries);
        model.addAttribute("search", search);
        model.addAttribute("category", category != null ? category : "ALL");
        model.addAttribute("sort", sort);
        model.addAttribute("view", view);
        model.addAttribute("categories", VaultEntry.Category.values());
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
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Incorrect master password");
            return "redirect:/vault/" + id;
        }
        VaultEntryDTO entry = vaultService.getEntryWithDecryptedPassword(user.getId(), id);
        model.addAttribute("entry", entry);
        model.addAttribute("revealed", true);
        model.addAttribute("user", user);
        return "vault/entry-detail";
    }

    @GetMapping("/add")
    public String addEntryPage(Model model) {
        User user = authUtil.getCurrentUser();
        model.addAttribute("entryDTO", new VaultEntryDTO());
        model.addAttribute("categories", VaultEntry.Category.values());
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
        if (result.hasErrors()) {
            model.addAttribute("entryDTO", dto);
            model.addAttribute("categories", VaultEntry.Category.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            model.addAttribute("entryDTO", dto);
            model.addAttribute("errorMsg", "Incorrect master password");
            model.addAttribute("categories", VaultEntry.Category.values());
            model.addAttribute("isEdit", false);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        vaultService.addEntry(user, dto);
        redirectAttrs.addFlashAttribute("successMsg", "Password entry added successfully!");
        return "redirect:/vault";
    }

    @GetMapping("/{id}/edit")
    public String editEntryPage(@PathVariable Long id, Model model) {
        User user = authUtil.getCurrentUser();
        VaultEntryDTO entry = vaultService.getEntryWithDecryptedPassword(user.getId(), id);
        model.addAttribute("entryDTO", entry);
        model.addAttribute("categories", VaultEntry.Category.values());
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
        if (result.hasErrors()) {
            model.addAttribute("entryDTO", dto);
            model.addAttribute("categories", VaultEntry.Category.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            model.addAttribute("entryDTO", dto);
            model.addAttribute("errorMsg", "Incorrect master password");
            model.addAttribute("categories", VaultEntry.Category.values());
            model.addAttribute("isEdit", true);
            model.addAttribute("user", user);
            return "vault/add-edit-entry";
        }
        vaultService.updateEntry(user.getId(), id, dto);
        redirectAttrs.addFlashAttribute("successMsg", "Entry updated!");
        return "redirect:/vault/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteEntry(@PathVariable Long id,
            @RequestParam String masterPassword,
            RedirectAttributes redirectAttrs) {
        User user = authUtil.getCurrentUser();
        if (!userService.verifyMasterPassword(user, masterPassword)) {
            redirectAttrs.addFlashAttribute("errorMsg", "Incorrect master password");
            return "redirect:/vault/" + id;
        }
        vaultService.deleteEntry(user.getId(), id);
        redirectAttrs.addFlashAttribute("successMsg", "Entry deleted");
        return "redirect:/vault";
    }

    @PostMapping("/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "/vault") String returnUrl) {
        User user = authUtil.getCurrentUser();
        vaultService.toggleFavorite(user.getId(), id);
        return "redirect:" + returnUrl;
    }
}
