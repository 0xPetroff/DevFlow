package com.devflow.project.dto;

import com.devflow.user.entity.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddMemberRequest(
        @NotNull UUID userId,
        @NotNull Role role) {
}
