package com.devflow.security;

import com.devflow.config.DevFlowProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final SecretKey KEY =
            new SecretKeySpec("a-test-signing-key-of-at-least-32-bytes!".getBytes(StandardCharsets.UTF_8), "HmacSHA256");

    private JwtService jwtService;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        DevFlowProperties properties = new DevFlowProperties(
                new DevFlowProperties.Jwt("ignored-secret", 15, 7, "devflow-test"),
                new DevFlowProperties.Cors(List.of()));
        jwtService = new JwtService(new NimbusJwtEncoder(new ImmutableSecret<>(KEY)), properties);
        decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Test
    void accessTokenCarriesIdentityClaimsAndVerifies() {
        UserPrincipal principal = principal();

        Jwt decoded = decoder.decode(jwtService.generateAccessToken(principal));

        assertThat(decoded.getSubject()).isEqualTo(principal.getId().toString());
        assertThat(decoded.getClaimAsString(JwtService.CLAIM_USERNAME)).isEqualTo("petar");
        assertThat(decoded.getClaimAsString(JwtService.CLAIM_EMAIL)).isEqualTo("petar@example.com");
        assertThat(decoded.getClaimAsString(JwtService.CLAIM_ROLE)).isEqualTo("DEVELOPER");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("devflow-test");
    }

    @Test
    void accessTokenExpiresAfterConfiguredWindow() {
        Jwt decoded = decoder.decode(jwtService.generateAccessToken(principal()));

        Duration lifetime = Duration.between(decoded.getIssuedAt(), decoded.getExpiresAt());
        assertThat(lifetime).isEqualTo(Duration.ofMinutes(15));
        assertThat(decoded.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        SecretKey otherKey = new SecretKeySpec(
                "a-completely-different-key-32-bytes-long".getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtDecoder foreignDecoder = NimbusJwtDecoder.withSecretKey(otherKey).macAlgorithm(MacAlgorithm.HS256).build();

        String token = jwtService.generateAccessToken(principal());

        assertThat(catchThrowable(() -> foreignDecoder.decode(token))).isNotNull();
    }

    @Test
    void refreshTokensAreUniqueAndHashedDeterministically() {
        String first = jwtService.generateRefreshToken();
        String second = jwtService.generateRefreshToken();

        assertThat(first).isNotEqualTo(second);
        assertThat(jwtService.hashRefreshToken(first))
                .isEqualTo(jwtService.hashRefreshToken(first))
                .isNotEqualTo(jwtService.hashRefreshToken(second))
                .hasSize(64)
                .doesNotContain(first);
    }

    private static Throwable catchThrowable(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private UserPrincipal principal() {
        return new UserPrincipal(UUID.randomUUID(), "petar", "petar@example.com", null,
                com.devflow.user.entity.Role.DEVELOPER, true);
    }
}
