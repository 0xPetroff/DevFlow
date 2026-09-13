package com.devflow.deployment.dto;

import com.devflow.deployment.entity.DeploymentStatus;
import com.devflow.project.dto.ProjectSummary;
import com.devflow.user.dto.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record DeploymentResponse(
        UUID id,
        ProjectSummary project,
        EnvironmentResponse environment,
        String releaseVersion,
        String commitHash,
        String commitMessage,
        String branch,
        DeploymentStatus status,
        UserSummary triggeredBy,
        String triggeredByLabel,
        String pipelineUrl,
        String failureReason,
        Instant queuedAt,
        Instant startedAt,
        Instant finishedAt,
        Integer durationSeconds) {
}
