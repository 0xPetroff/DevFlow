package com.devflow.deployment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * The environment is named rather than identified by id: this is the request a CI workflow makes,
 * and a pipeline has "staging" to hand far more naturally than a UUID. Names are unique per
 * project, so the pair still resolves to exactly one environment.
 */
public record CreateDeploymentRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 60) String environmentName,
        @NotBlank @Size(max = 60) String releaseVersion,
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{7,40}$", message = "must be a git commit hash")
        String commitHash,
        @Size(max = 500) String commitMessage,
        @NotBlank @Size(max = 200) String branch,
        @Size(max = 500) String pipelineUrl) {
}
