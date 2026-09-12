package com.devflow.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "devflow")
public record DevFlowProperties(@Valid @NotNull Jwt jwt, @Valid @NotNull Cors cors) {

    public record Jwt(
            /*
             * HMAC-SHA256 signing key. Must decode to at least 32 bytes; SecurityConfig fails
             * fast on startup rather than silently signing with a weak key.
             */
            @NotBlank String secret,
            @Min(1) int accessTokenMinutes,
            @Min(1) int refreshTokenDays,
            @NotBlank String issuer) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
