package com.devflow.security;

import com.devflow.user.entity.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Rebuilds a UserPrincipal from token claims so authorization needs no database round trip.
 * The cost is that a role change or deactivation only takes effect once the short-lived
 * access token expires; refresh tokens are DB-backed and revoked immediately.
 */
@Component
public class JwtUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserPrincipal principal = new UserPrincipal(
                UUID.fromString(jwt.getSubject()),
                jwt.getClaimAsString(JwtService.CLAIM_USERNAME),
                jwt.getClaimAsString(JwtService.CLAIM_EMAIL),
                null,
                Role.valueOf(jwt.getClaimAsString(JwtService.CLAIM_ROLE)),
                true);
        return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
    }
}
