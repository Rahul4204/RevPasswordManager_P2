package com.passwordmanager.app.config;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.IUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LogManager.getLogger(CustomAuthenticationSuccessHandler.class);

    private final IUserRepository userRepository;

    public CustomAuthenticationSuccessHandler(IUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();
        Optional<User> optUser = userRepository.findByUsername(username);

        if (optUser.isPresent() && optUser.get().isTotpEnabled()) {
            User user = optUser.get();
            // Store pending userId and clear authentication so user is not fully logged in
            // yet
            HttpSession session = request.getSession(true);
            session.setAttribute("pending2faUserId", user.getId());
            SecurityContextHolder.clearContext();
            logger.info("User {} has 2FA enabled. Redirecting to 2FA verification.", username);
            response.sendRedirect("/auth/2fa-login");
        } else {
            logger.info("User {} logged in successfully. Redirecting to dashboard.", username);
            response.sendRedirect("/dashboard");
        }
    }
}
