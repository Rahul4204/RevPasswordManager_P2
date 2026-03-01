package com.passwordmanager.app.controller;
import com.passwordmanager.app.dto.RegisterDTO;
import com.passwordmanager.app.service.IPasswordRecoveryService; import com.passwordmanager.app.service.IUserService; import com.passwordmanager.app.service.IVerificationService;
import org.junit.Before; import org.junit.Test; import org.mockito.Mock; import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc; import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*; import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*; import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
public class AuthControllerTest {
    private MockMvc mockMvc; @Mock private IUserService us; @Mock private IPasswordRecoveryService rs; @Mock private IVerificationService vs;
    @Before public void setUp() { MockitoAnnotations.initMocks(this); mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(us,rs,vs)).build(); }
    @Test public void testLoginPage() throws Exception { mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login")); }
    @Test public void testRegisterPage() throws Exception { mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("auth/register")).andExpect(model().attributeExists("registerDTO")); }
    @Test public void testDoRegisterSuccess() throws Exception {
        doNothing().when(us).preValidateRegistration(any(RegisterDTO.class)); when(vs.sendRegistrationOtp(anyString())).thenReturn("123456");
        mockMvc.perform(post("/register").param("username","newuser").param("email","new@example.com").param("fullName","New User")
            .param("masterPassword","password123").param("confirmPassword","password123")
            .param("securityQuestions[0].questionText","Question One").param("securityQuestions[0].answer","AnswerOne")
            .param("securityQuestions[1].questionText","Question Two").param("securityQuestions[1].answer","AnswerTwo")
            .param("securityQuestions[2].questionText","Question Three").param("securityQuestions[2].answer","AnswerThree"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/auth/verify-email"));
    }
    @Test public void testRecoverPage() throws Exception { mockMvc.perform(get("/recover")).andExpect(status().isOk()).andExpect(view().name("auth/recover")); }
}