package com.devflow.user.dto;

import com.devflow.user.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 120) String fullName,
        @NotNull Role role,
        @NotNull Boolean active) {
}
