package com.devflow.apikey.dto;

/**
 * The creation response, and the only one that ever carries {@code key}. Only its hash is stored,
 * so a key that is not copied now cannot be recovered later.
 */
public record IssuedApiKeyResponse(
        ApiKeyResponse apiKey,
        String key) {
}
