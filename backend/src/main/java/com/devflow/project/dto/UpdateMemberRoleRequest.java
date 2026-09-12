package com.devflow.project.dto;

import com.devflow.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull Role role) {
}
