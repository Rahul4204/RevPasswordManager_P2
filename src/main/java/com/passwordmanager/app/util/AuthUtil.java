package com.passwordmanager.app.util;

import com.passwordmanager.app.entity.User;
import com.passwordmanager.app.repository.IUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    private final IUserRepository IUserRepository;

    public AuthUtil(IUserRepository IUserRepository) {
        this.IUserRepository = IUserRepository;
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;
        return IUserRepository.findByUsername(auth.getName()).orElse(null);
    }

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }
}
