package com.passwordmanager.app.rest;
import com.passwordmanager.app.entity.User; import com.passwordmanager.app.service.VaultService; import com.passwordmanager.app.util.AuthUtil;
import org.junit.Before; import org.junit.Test; import org.mockito.Mock; import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType; import org.springframework.test.web.servlet.MockMvc; import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class VaultRestControllerTest {
    private MockMvc mockMvc; @Mock private VaultService vs; @Mock private AuthUtil au;
    @Before public void setUp() { MockitoAnnotations.initMocks(this); mockMvc = MockMvcBuilders.standaloneSetup(new VaultRestController(vs,au)).build(); User u = new User(); u.setId(1L); when(au.getCurrentUser()).thenReturn(u); }
    @Test public void testGetAllEntries() throws Exception { when(vs.getAllEntries(anyLong(),any(),any(),any())).thenReturn(new ArrayList<>()); mockMvc.perform(get("/api/vault")).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON)); }
    @Test public void testAddEntry() throws Exception { mockMvc.perform(post("/api/vault").contentType(MediaType.APPLICATION_JSON).content("{\"accountName\":\"T\",\"password\":\"p\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.message").value("Entry added successfully")); }
    @Test public void testDeleteEntry() throws Exception { mockMvc.perform(delete("/api/vault/1")).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Entry deleted successfully")); }
}