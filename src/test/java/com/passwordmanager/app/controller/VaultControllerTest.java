package com.passwordmanager.app.controller;
import com.passwordmanager.app.dto.VaultEntryDTO; import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.service.IUserService; import com.passwordmanager.app.service.IVaultService; import com.passwordmanager.app.util.AuthUtil;
import org.junit.Before; import org.junit.Test; import org.mockito.Mock; import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc; import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.ArrayList;
import static org.mockito.Mockito.*; import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class VaultControllerTest {
    private MockMvc mockMvc; @Mock private IVaultService vs; @Mock private IUserService us; @Mock private AuthUtil au;
    @Before public void setUp() {
        MockitoAnnotations.initMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(new VaultController(vs,us,au)).build();
        User u = new User(); u.setId(1L); u.setUsername("t"); when(au.getCurrentUser()).thenReturn(u);
    }
    @Test public void testVaultList() throws Exception {
        when(vs.getAllEntries(anyLong(),any(),any(),any())).thenReturn(new ArrayList<>());
        mockMvc.perform(get("/vault")).andExpect(status().isOk()).andExpect(view().name("vault/vault")).andExpect(model().attributeExists("entries","categories"));
    }
    @Test public void testViewEntry() throws Exception {
        VaultEntryDTO dto = new VaultEntryDTO(); dto.setId(1L); when(vs.getEntryMasked(1L,1L)).thenReturn(dto);
        mockMvc.perform(get("/vault/1")).andExpect(status().isOk()).andExpect(view().name("vault/entry-detail")).andExpect(model().attribute("entry",dto));
    }
    @Test public void testDeleteEntry() throws Exception {
        when(us.verifyMasterPassword(any(),anyString())).thenReturn(true);
        mockMvc.perform(post("/vault/1/delete").param("masterPassword","s")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/vault"));
        verify(vs).deleteEntry(anyLong(),eq(1L));
    }
}