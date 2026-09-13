package com.devflow.user.dto;

import java.util.UUID;

/** Nested representation used wherever a user appears inside another resource. */
public record UserSummary(
        UUID id,
        String username,
        String fullName,
        String avatarColor) {
}
