package com.devflow.audit.repository;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.entity.AuditLog;
import com.devflow.common.Specifications;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Specifications rather than a JPQL query with ":param IS NULL OR ..." branches, for the reason
 * spelled out on UserSpecifications: PostgreSQL cannot infer the type of an untyped null bind.
 */
public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> matching(UUID projectId, UUID actorId, AuditAction action,
                                                   Instant since, String search) {
        return Specifications.allOfPresent(inProject(projectId), byActor(actorId), hasAction(action),
                recordedSince(since), matchesText(search));
    }

    private static Specification<AuditLog> inProject(UUID projectId) {
        return projectId == null ? null : (root, query, cb) -> cb.equal(root.get("projectId"), projectId);
    }

    private static Specification<AuditLog> byActor(UUID actorId) {
        return actorId == null ? null : (root, query, cb) -> cb.equal(root.get("actorId"), actorId);
    }

    private static Specification<AuditLog> hasAction(AuditAction action) {
        return action == null ? null : (root, query, cb) -> cb.equal(root.get("action"), action);
    }

    private static Specification<AuditLog> recordedSince(Instant since) {
        return since == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }

    private static Specification<AuditLog> matchesText(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("summary")), pattern),
                cb.like(cb.lower(root.get("actorLabel")), pattern));
    }
}
