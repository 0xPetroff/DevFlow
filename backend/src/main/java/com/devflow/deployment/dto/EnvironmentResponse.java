package com.devflow.deployment.dto;

import com.devflow.deployment.entity.EnvironmentType;

import java.time.Instant;
import java.util.UUID;

public record EnvironmentResponse(
        UUID id,
        String name,
        EnvironmentType type,
        String url,
        boolean requiresApproval,
        Instant createdAt,
        Instant updatedAt) {
}
