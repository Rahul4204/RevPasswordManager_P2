package com.passwordmanager.app.rest;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import com.passwordmanager.app.dto.PasswordResultDTO;
import com.passwordmanager.app.service.PasswordGeneratorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/generator")
public class PasswordGeneratorRestController {

    private final PasswordGeneratorService generatorService;

    public PasswordGeneratorRestController(PasswordGeneratorService generatorService) {
        this.generatorService = generatorService;
    }

    @PostMapping("/generate")
    public ResponseEntity<List<PasswordResultDTO>> generate(@RequestBody PasswordGeneratorConfigDTO config) {
        List<String> passwords = generatorService.generate(config);
        List<PasswordResultDTO> result = passwords.stream().map(pw -> {
            int score = generatorService.strengthScore(pw);
            String label = generatorService.strengthLabel(score);
            return new PasswordResultDTO(pw, score, label);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
