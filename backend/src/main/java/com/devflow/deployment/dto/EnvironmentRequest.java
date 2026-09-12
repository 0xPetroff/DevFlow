package com.devflow.deployment.dto;

import com.devflow.deployment.entity.EnvironmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnvironmentRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull EnvironmentType type,
        @Size(max = 500) String url,
        // Null is treated as false; only production usually wants the manual gate.
        Boolean requiresApproval) {
}
