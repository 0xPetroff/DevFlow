package com.devflow.deployment.repository;

import com.devflow.common.Specifications;
import com.devflow.deployment.entity.Deployment;
import com.devflow.deployment.entity.DeploymentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public final class DeploymentSpecifications {

    private DeploymentSpecifications() {
    }

    public static Specification<Deployment> matching(UUID projectId, Collection<DeploymentStatus> statuses,
                                                     UUID environmentId, String branch, String search) {
        return Specifications.allOfPresent(
                inProject(projectId),
                hasStatus(statuses),
                inEnvironment(environmentId),
                onBranch(branch),
                matchesText(search));
    }

    private static Specification<Deployment> inProject(UUID projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    private static Specification<Deployment> hasStatus(Collection<DeploymentStatus> statuses) {
        return statuses == null || statuses.isEmpty()
                ? null
                : (root, query, cb) -> root.get("status").in(statuses);
    }

    private static Specification<Deployment> inEnvironment(UUID environmentId) {
        return environmentId == null
                ? null
                : (root, query, cb) -> cb.equal(root.get("environment").get("id"), environmentId);
    }

    private static Specification<Deployment> onBranch(String branch) {
        return branch == null || branch.isBlank()
                ? null
                : (root, query, cb) -> cb.equal(root.get("branch"), branch.trim());
    }

    private static Specification<Deployment> matchesText(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("releaseVersion")), pattern),
                cb.like(cb.lower(root.get("commitHash")), pattern));
    }
}
