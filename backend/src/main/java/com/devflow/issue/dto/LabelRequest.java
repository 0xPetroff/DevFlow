package com.devflow.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LabelRequest(
        @NotBlank @Size(max = 40) String name,
        @NotBlank @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "must be a hex colour such as #6366f1")
        String color) {
}
