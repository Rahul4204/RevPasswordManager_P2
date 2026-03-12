package com.passwordmanager.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ThymeleafContextFilter extends OncePerRequestFilter {

    @Autowired
    private ServletContext servletContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            @Override
            public ServletContext getServletContext() {
                return servletContext;
            }
        };

        filterChain.doFilter(wrapper, response);
    }
}
