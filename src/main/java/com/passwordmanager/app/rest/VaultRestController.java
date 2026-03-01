package com.passwordmanager.app.rest;

import com.passwordmanager.app.dto.VaultEntryDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.service.VaultService;
import com.passwordmanager.app.util.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vault")
public class VaultRestController {

    private final VaultService vaultService;
    private final AuthUtil authUtil;

    public VaultRestController(VaultService vaultService, AuthUtil authUtil) {
        this.vaultService = vaultService;
        this.authUtil = authUtil;
    }

    @GetMapping
    public ResponseEntity<List<VaultEntryDTO>> getAllEntries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "name") String sort) {
        User user = authUtil.getCurrentUser();
        List<VaultEntryDTO> entries = vaultService.getAllEntries(user.getId(), search, category, sort);
        return ResponseEntity.ok(entries);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> addEntry(@RequestBody VaultEntryDTO dto) {
        User user = authUtil.getCurrentUser();
        vaultService.addEntry(user, dto);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Entry added successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateEntry(@PathVariable Long id, @RequestBody VaultEntryDTO dto) {
        User user = authUtil.getCurrentUser();
        vaultService.updateEntry(user.getId(), id, dto);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Entry updated successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEntry(@PathVariable Long id) {
        User user = authUtil.getCurrentUser();
        vaultService.deleteEntry(user.getId(), id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Entry deleted successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Map<String, String>> toggleFavorite(@PathVariable Long id) {
        User user = authUtil.getCurrentUser();
        vaultService.toggleFavorite(user.getId(), id);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Favorite toggled");
        return ResponseEntity.ok(response);
    }
}
