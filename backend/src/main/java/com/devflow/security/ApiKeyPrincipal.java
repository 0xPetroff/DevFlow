package com.devflow.security;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated identity of a build agent. It is deliberately not a UserPrincipal: a key is
 * not a person, holds no role, and can only ever act on the one project it was issued for.
 */
public record ApiKeyPrincipal(UUID keyId, String name, UUID projectId, Set<String> scopes) {

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }

    public boolean isForProject(UUID candidate) {
        return projectId != null && projectId.equals(candidate);
    }

    /** Used as the audit actor label, so a CI-triggered change is never attributed to a person. */
    public String auditLabel() {
        return "api-key:" + name;
    }
}
