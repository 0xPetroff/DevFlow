package com.devflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<UserPrincipal> principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof UserPrincipal principal
                ? Optional.of(principal)
                : Optional.empty();
    }

    public static UserPrincipal principalOrNull() {
        return principal().orElse(null);
    }

    public static Optional<UUID> id() {
        return principal().map(UserPrincipal::getId);
    }
}
