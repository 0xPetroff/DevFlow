package com.devflow.issue.repository;

import com.devflow.common.Specifications;
import com.devflow.issue.dto.IssueFilter;
import com.devflow.issue.entity.Issue;
import com.devflow.issue.entity.IssuePriority;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.IssueType;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

/**
 * Specifications rather than JPQL with ":param IS NULL OR ..." branches: PostgreSQL cannot infer
 * the type of an untyped null bind and fails with "function lower(bytea) does not exist".
 */
public final class IssueSpecifications {

    private IssueSpecifications() {
    }

    public static Specification<Issue> matching(UUID projectId, IssueFilter filter) {
        return Specifications.allOfPresent(
                inProject(projectId),
                hasStatus(filter.statuses()),
                hasPriority(filter.priorities()),
                hasType(filter.types()),
                hasAssignee(filter.assigneeId(), filter.unassigned()),
                hasLabel(filter.labelId()),
                dueBefore(filter.dueBefore()),
                matchesText(filter.search()));
    }

    private static Specification<Issue> inProject(UUID projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    private static Specification<Issue> hasStatus(Collection<IssueStatus> statuses) {
        return isEmpty(statuses) ? null : (root, query, cb) -> root.get("status").in(statuses);
    }

    private static Specification<Issue> hasPriority(Collection<IssuePriority> priorities) {
        return isEmpty(priorities) ? null : (root, query, cb) -> root.get("priority").in(priorities);
    }

    private static Specification<Issue> hasType(Collection<IssueType> types) {
        return isEmpty(types) ? null : (root, query, cb) -> root.get("type").in(types);
    }

    private static Specification<Issue> hasAssignee(UUID assigneeId, Boolean unassigned) {
        if (Boolean.TRUE.equals(unassigned)) {
            return (root, query, cb) -> cb.isNull(root.get("assignee"));
        }
        return assigneeId == null ? null : (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    private static Specification<Issue> hasLabel(UUID labelId) {
        // An inner join to the label collection: filtering on a single id cannot multiply rows.
        return labelId == null ? null : (root, query, cb) ->
                cb.equal(root.join("labels", JoinType.INNER).get("id"), labelId);
    }

    private static Specification<Issue> dueBefore(LocalDate date) {
        return date == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), date);
    }

    private static Specification<Issue> matchesText(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern));
    }

    private static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
