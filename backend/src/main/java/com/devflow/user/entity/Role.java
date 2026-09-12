package com.devflow.user.entity;

/**
 * Account-wide role. Project-scoped permissions are held separately on ProjectMember;
 * ADMIN here is the only role that bypasses project membership checks.
 *
 * <p>The same three values describe both tiers, so the rank below is also what
 * ProjectAccessService compares a member's project role against.
 */
public enum Role {
    ADMIN(30),
    DEVELOPER(20),
    VIEWER(10);

    private final int rank;

    Role(int rank) {
        this.rank = rank;
    }

    /** Explicit ranks rather than ordinal comparison, so reordering the constants cannot silently grant access. */
    public boolean isAtLeast(Role required) {
        return rank >= required.rank;
    }
}
