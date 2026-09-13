package com.devflow.apikey.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(
        @NotBlank @Size(max = 120) String name,
        // Null means the key never expires, which is a deliberate choice rather than a default.
        @Min(1) @Max(3650) Integer expiresInDays) {
}
