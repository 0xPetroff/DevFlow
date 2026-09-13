package com.devflow.project.dto;

import com.devflow.project.entity.ProjectStatus;

import java.util.UUID;

/** Nested representation used wherever a project appears inside another resource. */
public record ProjectSummary(
        UUID id,
        String projectKey,
        String name,
        ProjectStatus status) {
}
