package com.devflow.project.repository;

import com.devflow.common.Specifications;
import com.devflow.project.entity.Project;
import com.devflow.project.entity.ProjectMember;
import com.devflow.project.entity.ProjectStatus;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

/**
 * Specifications rather than JPQL with ":param IS NULL OR ..." branches: PostgreSQL cannot infer
 * the type of an untyped null bind and fails with "function lower(bytea) does not exist".
 */
public final class ProjectSpecifications {

    private ProjectSpecifications() {
    }

    public static Specification<Project> matching(String search, ProjectStatus status, UUID visibleToUserId) {
        return Specifications.allOfPresent(hasStatus(status), matchesText(search), visibleTo(visibleToUserId));
    }

    private static Specification<Project> hasStatus(ProjectStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    private static Specification<Project> matchesText(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("projectKey")), pattern));
    }

    /**
     * Restricts a listing to projects the user owns or belongs to. A null id means no restriction,
     * which is only ever passed for an account-wide ADMIN.
     */
    private static Specification<Project> visibleTo(UUID userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<UUID> membership = query.subquery(UUID.class);
            Root<ProjectMember> member = membership.from(ProjectMember.class);
            membership.select(member.get("id"))
                    .where(cb.equal(member.get("project").get("id"), root.get("id")),
                            cb.equal(member.get("user").get("id"), userId));
            return cb.or(cb.equal(root.get("owner").get("id"), userId), cb.exists(membership));
        };
    }
}
