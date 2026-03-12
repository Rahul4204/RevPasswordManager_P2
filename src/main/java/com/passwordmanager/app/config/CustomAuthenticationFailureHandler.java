package com.passwordmanager.app.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger logger = LogManager.getLogger(CustomAuthenticationFailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        logger.error("Authentication failed: {}", exception.getMessage());

        String errorMessage = "true"; // Default generic error

        // Check for JPA / DB related errors in the exception cause chain
        Throwable cause = exception.getCause();
        while (cause != null) {
            String msg = cause.getMessage();
            if (msg != null && (msg.contains("JPA") || msg.contains("Entity manager") || msg.contains("Connection")
                    || msg.contains("DataSource"))) {
                logger.error("Detected database connection issue during login: {}", msg);
                errorMessage = "dbDown";
                break;
            }
            cause = cause.getCause();
        }

        setDefaultFailureUrl("/login?error=" + errorMessage);
        super.onAuthenticationFailure(request, response, exception);
    }
}
