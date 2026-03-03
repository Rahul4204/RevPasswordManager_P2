package com.passwordmanager.app.dto;

import lombok.Data;

@Data
public class PasswordGeneratorConfigDTO {
    private int length = 16;
    private boolean includeUppercase = true;
    private boolean includeLowercase = true;
    private boolean includeNumbers = true;
    private boolean includeSymbols = true;
    private boolean excludeSimilar = false;
    private int count = 1; // how many passwords to generate
}
