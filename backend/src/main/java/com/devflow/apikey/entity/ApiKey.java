package com.devflow.apikey.entity;

import com.devflow.common.BaseEntity;
import com.devflow.project.entity.Project;
import com.devflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A credential for build agents. Only the SHA-256 hash of the key is stored, so a database leak
 * hands out nothing usable; the indexed prefix is what makes the lookup possible without it.
 */
@Entity
@Table(name = "api_keys")
public class ApiKey extends BaseEntity {

    public static final String SCOPE_DEPLOYMENT_WRITE = "deployment:write";

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 16, updatable = false)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, length = 64, updatable = false)
    private String keyHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", updatable = false)
    private Project project;

    @Column(name = "scopes", nullable = false, length = 255)
    private String scopes = SCOPE_DEPLOYMENT_WRITE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected ApiKey() {
    }

    public ApiKey(String name, String keyPrefix, String keyHash, Project project,
                  User createdBy, Instant expiresAt) {
        this.name = name;
        this.keyPrefix = keyPrefix;
        this.keyHash = keyHash;
        this.project = project;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return revokedAt == null && (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    public boolean hasScope(String scope) {
        return scopeSet().contains(scope);
    }

    public Set<String> scopeSet() {
        return Arrays.stream(scopes.split(",")).map(String::trim)
                .filter(scope -> !scope.isEmpty())
                .collect(Collectors.toSet());
    }

    public void revoke() {
        if (revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public void recordUse() {
        this.lastUsedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public Project getProject() {
        return project;
    }

    public String getScopes() {
        return scopes;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }
}
