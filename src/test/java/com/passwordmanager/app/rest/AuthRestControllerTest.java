package com.passwordmanager.app.rest;

import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.service.UserService;
import com.passwordmanager.app.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthRestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager am;
    @Mock
    private UserService us;
    @Mock
    private JwtUtil jwt;
    @Mock
    private UserDetailsService uds;
    @Mock
    private Authentication auth;
    @Mock
    private UserDetails ud;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthRestController(am, us, jwt, uds)).build();
    }

    @Test
    void testLogin() throws Exception {
        when(am.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(auth.getName()).thenReturn("u");
        when(uds.loadUserByUsername("u")).thenReturn(ud);
        when(jwt.generateToken(ud)).thenReturn("tok");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"u\",\"masterPassword\":\"p\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"));
    }

    @Test
    void testRegister() throws Exception {
        User u = new User();
        u.setUsername("nu");
        when(us.register(any(RegisterDTO.class))).thenReturn(u);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nu\",\"email\":\"n@t.com\",\"masterPassword\":\"p\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }
}