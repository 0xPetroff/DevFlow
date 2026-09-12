package com.devflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class CurrentApiKey {

    private CurrentApiKey() {
    }

    public static Optional<ApiKeyPrincipal> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof ApiKeyPrincipal principal
                ? Optional.of(principal)
                : Optional.empty();
    }
}
