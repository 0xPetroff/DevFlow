package com.devflow.project.dto;

import com.devflow.project.entity.ProjectStatus;
import com.devflow.user.dto.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String projectKey,
        String name,
        String description,
        String repositoryUrl,
        ProjectStatus status,
        UserSummary owner,
        long memberCount,
        Instant createdAt,
        Instant updatedAt) {
}
