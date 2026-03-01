package com.passwordmanager.app.rest;
import com.passwordmanager.app.dto.PasswordGeneratorConfigDTO; import com.passwordmanager.app.service.PasswordGeneratorService;
import org.junit.Before; import org.junit.Test; import org.mockito.Mock; import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc; import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Arrays;
import static org.mockito.ArgumentMatchers.any; import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class PasswordGeneratorRestControllerTest {
    private MockMvc mockMvc; @Mock private PasswordGeneratorService gs;
    @Before public void setUp() { MockitoAnnotations.initMocks(this); mockMvc = MockMvcBuilders.standaloneSetup(new PasswordGeneratorRestController(gs)).build(); }
    @Test public void testGenerate() throws Exception {
        when(gs.generate(any(PasswordGeneratorConfigDTO.class))).thenReturn(Arrays.asList("mock-pass")); when(gs.strengthScore("mock-pass")).thenReturn(4); when(gs.strengthLabel(4)).thenReturn("Very Strong");
        mockMvc.perform(post("/api/generator/generate").contentType(MediaType.APPLICATION_JSON).content("{\"length\":16}")).andExpect(status().isOk()).andExpect(jsonPath("$[0].password").value("mock-pass")).andExpect(jsonPath("$[0].score").value(4));
    }
}