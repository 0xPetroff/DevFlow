package com.devflow.deployment;

import com.devflow.deployment.entity.DeploymentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentStatusTest {

    @Test
    void aQueuedRunCanOnlyStartOrBeCancelled() {
        assertThat(DeploymentStatus.PENDING.allowedNext())
                .containsExactlyInAnyOrder(DeploymentStatus.RUNNING, DeploymentStatus.CANCELLED);
        assertThat(DeploymentStatus.PENDING.canTransitionTo(DeploymentStatus.SUCCESS)).isFalse();
    }

    @Test
    void aRunningDeploymentCanOnlyReachAnOutcome() {
        assertThat(DeploymentStatus.RUNNING.allowedNext()).containsExactlyInAnyOrder(
                DeploymentStatus.SUCCESS, DeploymentStatus.FAILED, DeploymentStatus.CANCELLED);
        assertThat(DeploymentStatus.RUNNING.canTransitionTo(DeploymentStatus.PENDING)).isFalse();
    }

    @Test
    void outcomesAreFinal() {
        for (DeploymentStatus terminal : new DeploymentStatus[]{
                DeploymentStatus.SUCCESS, DeploymentStatus.FAILED, DeploymentStatus.CANCELLED}) {
            assertThat(terminal.isTerminal()).isTrue();
            assertThat(terminal.allowedNext()).isEmpty();
            for (DeploymentStatus next : DeploymentStatus.values()) {
                assertThat(terminal.canTransitionTo(next)).isFalse();
            }
        }
    }

    @Test
    void noStatusCanTransitionToItself() {
        for (DeploymentStatus status : DeploymentStatus.values()) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }
    }

    @Test
    void onlyOutcomesCountAsFinished() {
        assertThat(DeploymentStatus.PENDING.isFinished()).isFalse();
        assertThat(DeploymentStatus.RUNNING.isFinished()).isFalse();
        assertThat(DeploymentStatus.SUCCESS.isFinished()).isTrue();
        assertThat(DeploymentStatus.CANCELLED.isFinished()).isTrue();
    }
}
