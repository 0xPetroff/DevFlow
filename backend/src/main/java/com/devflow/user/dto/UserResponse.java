package com.devflow.user.dto;

import com.devflow.user.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String fullName,
        Role role,
        String avatarColor,
        boolean active,
        Instant createdAt,
        Instant lastLoginAt) {
}
