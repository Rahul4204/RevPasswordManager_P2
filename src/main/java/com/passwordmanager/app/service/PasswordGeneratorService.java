package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PasswordGeneratorService implements IPasswordGeneratorService {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // excludes O, I by default (similar chars)
    private static final String UPPER_FULL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghjkmnpqrstuvwxyz"; // excludes l, 1 by default
    private static final String LOWER_FULL = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "23456789"; // excludes 0,1 similar to O,l
    private static final String DIGITS_FULL = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?/";
    private static final int MIN_LENGTH = 8;

    private final SecureRandom random = new SecureRandom();

    @Override
    public List<String> generate(PasswordGeneratorConfigDTO config) {
        List<String> passwords = new ArrayList<>();
        int count = config.getCount() > 0 ? config.getCount() : 1;

        for (int i = 0; i < count; i++) {
            passwords.add(generateSingle(config));
        }
        return passwords;
    }

    private String generateSingle(PasswordGeneratorConfigDTO config) {
        boolean excludeSimilar = config.isExcludeSimilar();

        String upper = excludeSimilar ? UPPER : UPPER_FULL;
        String lower = excludeSimilar ? LOWER : LOWER_FULL;
        String digits = excludeSimilar ? DIGITS : DIGITS_FULL;

        StringBuilder chars = new StringBuilder();
        StringBuilder mandatory = new StringBuilder();

        if (config.isIncludeUppercase()) {
            chars.append(upper);
            mandatory.append(upper.charAt(random.nextInt(upper.length())));
        }
        if (config.isIncludeLowercase()) {
            chars.append(lower);
            mandatory.append(lower.charAt(random.nextInt(lower.length())));
        }
        if (config.isIncludeNumbers()) {
            chars.append(digits);
            mandatory.append(digits.charAt(random.nextInt(digits.length())));
        }
        if (config.isIncludeSymbols()) {
            chars.append(SYMBOLS);
            mandatory.append(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
        }

        if (chars.length() == 0) {
            chars.append(lower.isEmpty() ? LOWER_FULL : lower);
            mandatory.append(chars.charAt(random.nextInt(chars.length())));
        }

        // Enforce minimum length of 8
        int length = Math.max(config.getLength(), MIN_LENGTH);

        while (mandatory.length() < length) {
            mandatory.append(chars.charAt(random.nextInt(chars.length())));
        }

        // Shuffle
        List<Character> pwdChars = new ArrayList<>();
        for (char c : mandatory.toString().toCharArray())
            pwdChars.add(c);
        Collections.shuffle(pwdChars, random);

        StringBuilder shuffled = new StringBuilder();
        for (char c : pwdChars)
            shuffled.append(c);

        return shuffled.toString();
    }

    @Override
    public int strengthScore(String password) {
        if (password == null || password.isEmpty())
            return 0;
        int score = 0;
        if (password.length() >= 8)
            score++;
        if (password.length() >= 12)
            score++;
        if (password.matches(".*[A-Z].*"))
            score++;
        if (password.matches(".*[a-z].*"))
            score++;
        if (password.matches(".*[0-9].*"))
            score++;
        if (password.matches(".*[!@#$%^&*()\\-_=+\\[\\]{}|;:,.<>?/].*"))
            score++;
        return Math.min(score, 4); // max score is 4
    }

    @Override
    public String strengthLabel(int score) {
        if (score <= 1)
            return "Weak";
        if (score == 2)
            return "Medium";
        if (score == 3)
            return "Strong";
        return "Very Strong";
    }
}
