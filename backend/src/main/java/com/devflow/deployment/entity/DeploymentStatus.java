package com.devflow.deployment.entity;

import java.util.Set;

/**
 * The deployment lifecycle. Transitions are declared here rather than checked at each call site,
 * so a build agent cannot report SUCCESS for a run it never started, and a finished run can never
 * be reopened.
 */
public enum DeploymentStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    CANCELLED;

    public Set<DeploymentStatus> allowedNext() {
        return switch (this) {
            case PENDING -> Set.of(RUNNING, CANCELLED);
            case RUNNING -> Set.of(SUCCESS, FAILED, CANCELLED);
            case SUCCESS, FAILED, CANCELLED -> Set.of();
        };
    }

    public boolean canTransitionTo(DeploymentStatus next) {
        return allowedNext().contains(next);
    }

    public boolean isTerminal() {
        return allowedNext().isEmpty();
    }

    /** CANCELLED straight from PENDING never ran, so it has no duration to record. */
    public boolean isFinished() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
}
