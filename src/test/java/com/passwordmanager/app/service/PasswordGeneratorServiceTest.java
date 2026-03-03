package com.passwordmanager.app.service;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorServiceTest {

    private PasswordGeneratorService service;

    @BeforeEach
    void setUp() {
        service = new PasswordGeneratorService();
    }

    @Test
    void testGenerateDefault() {
        List<String> r = service.generate(new PasswordGeneratorConfigDTO());
        assertEquals(1, r.size());
        assertEquals(16, r.get(0).length());
        assertTrue(r.get(0).matches(".*[A-Z].*"));
        assertTrue(r.get(0).matches(".*[a-z].*"));
        assertTrue(r.get(0).matches(".*[0-9].*"));
    }

    @Test
    void testGenerateMultiple() {
        PasswordGeneratorConfigDTO c = new PasswordGeneratorConfigDTO();
        c.setCount(5);
        assertEquals(5, service.generate(c).size());
    }

    @Test
    void testMinimumLength() {
        PasswordGeneratorConfigDTO c = new PasswordGeneratorConfigDTO();
        c.setLength(4);
        assertEquals(8, service.generate(c).get(0).length());
    }

    @Test
    void testStrengthScore() {
        assertEquals(4, service.strengthScore("ComplexP@ssw0rd123!"));
        assertEquals(0, service.strengthScore(""));
    }

    @Test
    void testStrengthLabel() {
        assertEquals("Weak", service.strengthLabel(0));
        assertEquals("Medium", service.strengthLabel(2));
        assertEquals("Strong", service.strengthLabel(3));
        assertEquals("Very Strong", service.strengthLabel(4));
    }
}