package com.devflow.security;

import com.devflow.common.Sha256;
import com.devflow.config.DevFlowProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_EMAIL = "email";

    private final JwtEncoder jwtEncoder;
    private final DevFlowProperties.Jwt config;
    private final SecureRandom random = new SecureRandom();

    public JwtService(JwtEncoder jwtEncoder, DevFlowProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.config = properties.jwt();
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(config.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl()))
                .subject(principal.getId().toString())
                .claim(CLAIM_USERNAME, principal.getUsername())
                .claim(CLAIM_EMAIL, principal.getEmail())
                .claim(CLAIM_ROLE, principal.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Refresh tokens are opaque random strings rather than JWTs: they must be revocable,
     * and only their hash is ever stored.
     */
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String rawToken) {
        return sha256Hex(rawToken);
    }

    public Duration accessTokenTtl() {
        return Duration.ofMinutes(config.accessTokenMinutes());
    }

    public Duration refreshTokenTtl() {
        return Duration.ofDays(config.refreshTokenDays());
    }

    static String sha256Hex(String value) {
        return Sha256.hex(value);
    }
}
