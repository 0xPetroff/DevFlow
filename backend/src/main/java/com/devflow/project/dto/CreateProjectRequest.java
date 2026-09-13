package com.devflow.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        // Accepted in any case and upper-cased on write; the stored form is what the schema constrains.
        @NotBlank
        @Pattern(regexp = "^[A-Za-z][A-Za-z0-9]{1,9}$",
                message = "must be 2 to 10 letters or digits and start with a letter")
        String projectKey,

        @NotBlank @Size(max = 120) String name,
        @Size(max = 5000) String description,
        @Size(max = 500) String repositoryUrl) {
}
