package com.devflow.apikey.dto;

import com.devflow.user.dto.UserSummary;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Everything about a key except the key. */
public record ApiKeyResponse(
        UUID id,
        String name,
        String keyPrefix,
        Set<String> scopes,
        UserSummary createdBy,
        Instant lastUsedAt,
        Instant expiresAt,
        Instant revokedAt,
        boolean active,
        Instant createdAt) {
}
