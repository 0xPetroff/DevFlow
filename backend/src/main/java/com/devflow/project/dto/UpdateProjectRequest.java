package com.devflow.project.dto;

import com.devflow.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** The project key is deliberately absent: issue keys derive from it, so it is immutable. */
public record UpdateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 5000) String description,
        @Size(max = 500) String repositoryUrl,
        @NotNull ProjectStatus status,
        // Null leaves ownership untouched; any other value transfers it to that user.
        UUID ownerId) {
}
