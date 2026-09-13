package com.devflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Without these, Spring Security answers unauthenticated API calls with 403 and an empty body.
 * Both handlers emit the same RFC 9457 shape the rest of the API uses.
 */
@Component
public class ProblemAuthenticationHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, request, HttpStatus.UNAUTHORIZED, "Authentication required",
                "A valid access token is required to call this endpoint", "unauthenticated");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, request, HttpStatus.FORBIDDEN, "Access denied",
                "You do not have permission to perform this action", "access-denied");
    }

    private void write(HttpServletResponse response, HttpServletRequest request, HttpStatus status,
                       String title, String detail, String type) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"type":"https://devflow.dev/problems/%s","title":"%s","status":%d,\
                "detail":"%s","instance":"%s","timestamp":"%s"}"""
                .formatted(type, title, status.value(), detail, request.getRequestURI(), Instant.now()));
    }
}
