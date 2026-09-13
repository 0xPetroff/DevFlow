package com.devflow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "may only contain letters, digits, dot, underscore or hyphen")
        String username,

        @NotBlank
        @Size(min = 10, max = 100, message = "must be between 10 and 100 characters")
        @Pattern(regexp = ".*[A-Za-z].*", message = "must contain at least one letter")
        @Pattern(regexp = ".*\\d.*", message = "must contain at least one digit")
        String password,

        @NotBlank @Size(max = 120) String fullName) {
}
