package com.passwordmanager.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RevPasswordManagerP2ApplicationTest {

    @Test
    void testApplicationClassExists() {
        RevPasswordManagerP2Application app = new RevPasswordManagerP2Application();
        assertNotNull(app);
    }

    @Test
    void testMainClassAnnotationsPresent() {
        assertNotNull(RevPasswordManagerP2Application.class.getAnnotations());
        assertTrue(RevPasswordManagerP2Application.class.isAnnotationPresent(
                org.springframework.boot.autoconfigure.SpringBootApplication.class));
    }
}
