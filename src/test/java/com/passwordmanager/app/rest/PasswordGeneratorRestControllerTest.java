package com.passwordmanager.app.rest;

import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO;
import com.passwordmanager.app.service.PasswordGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PasswordGeneratorRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PasswordGeneratorService gs;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PasswordGeneratorRestController(gs)).build();
    }

    @Test
    void testGenerate() throws Exception {
        when(gs.generate(any(PasswordGeneratorConfigDTO.class))).thenReturn(Arrays.asList("mock-pass"));
        when(gs.strengthScore("mock-pass")).thenReturn(4);
        when(gs.strengthLabel(4)).thenReturn("Very Strong");

        mockMvc.perform(post("/api/generator/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"length\":16}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").value("mock-pass"))
                .andExpect(jsonPath("$[0].score").value(4));
    }
}