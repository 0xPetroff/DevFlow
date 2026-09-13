package com.devflow.user.repository;

import com.devflow.common.Specifications;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/**
 * Specifications rather than a JPQL query with ":param IS NULL OR ..." branches: PostgreSQL
 * cannot infer the type of an untyped null bind and fails with "function lower(bytea) does not
 * exist". Building the predicate list dynamically omits absent filters entirely.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> matching(String search, Role role, Boolean active) {
        return Specifications.allOfPresent(hasRole(role), isActive(active), matchesText(search));
    }

    private static Specification<User> hasRole(Role role) {
        return role == null ? null : (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    private static Specification<User> isActive(Boolean active) {
        return active == null ? null : (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    private static Specification<User> matchesText(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(cb.lower(root.get("username")), pattern),
                cb.like(cb.lower(root.get("email")), pattern));
    }
}
