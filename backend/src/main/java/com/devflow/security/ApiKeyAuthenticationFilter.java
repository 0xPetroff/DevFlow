package com.devflow.security;

import com.devflow.apikey.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authenticates build agents presenting {@code X-DevFlow-Api-Key}.
 *
 * <p>Scoped to the deployment endpoints on purpose: a CI key is issued for reporting releases, and
 * restricting it here means a leaked key cannot read the issue tracker or the user directory even
 * if a later endpoint forgets to check its scopes.
 *
 * <p>A key is ignored when the caller also sent an Authorization header, so a request is never
 * authenticated as two different identities and the bearer token always wins.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-DevFlow-Api-Key";
    private static final String PROTECTED_PATH = "/api/deployments";

    private final ApiKeyService apiKeyService;

    public ApiKeyAuthenticationFilter(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String presented = request.getHeader(HEADER);
        boolean available = presented != null && !presented.isBlank()
                && request.getHeader(HttpHeaders.AUTHORIZATION) == null
                && SecurityContextHolder.getContext().getAuthentication() == null;

        if (available) {
            // An unrecognised key simply leaves the request unauthenticated, so it fails at the
            // authorization rules like any other anonymous call rather than in a filter.
            apiKeyService.authenticate(presented).ifPresent(principal ->
                    SecurityContextHolder.getContext().setAuthentication(authenticationFor(principal)));
        }

        chain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PROTECTED_PATH);
    }

    private PreAuthenticatedAuthenticationToken authenticationFor(ApiKeyPrincipal principal) {
        Set<SimpleGrantedAuthority> authorities = principal.scopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .collect(Collectors.toSet());
        return new PreAuthenticatedAuthenticationToken(principal, "n/a", authorities);
    }
}
