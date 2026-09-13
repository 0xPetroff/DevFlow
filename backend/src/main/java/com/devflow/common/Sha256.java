package com.devflow.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hashing for bearer-style secrets that must be looked up by exact value: refresh tokens and API
 * keys. Deliberately not a password hash. These are long random strings rather than chosen
 * secrets, so there is nothing to brute force and every request would pay the work factor.
 */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required but unavailable", e);
        }
    }
}
