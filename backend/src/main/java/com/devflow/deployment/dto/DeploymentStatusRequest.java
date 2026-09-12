package com.devflow.deployment.dto;

import com.devflow.deployment.entity.DeploymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DeploymentStatusRequest(
        @NotNull DeploymentStatus status,
        @Size(max = 2000) String failureReason) {
}
