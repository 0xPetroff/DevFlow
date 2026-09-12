package com.devflow.deployment.entity;

import com.devflow.common.AuditedEntity;
import com.devflow.exception.BusinessRuleException;
import com.devflow.project.entity.Project;
import com.devflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;

@Entity
@Table(name = "deployments")
public class Deployment extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false, updatable = false)
    private Environment environment;

    @Column(name = "release_version", nullable = false, length = 60)
    private String releaseVersion;

    @Column(name = "commit_hash", nullable = false, length = 40)
    private String commitHash;

    @Column(name = "commit_message", length = 500)
    private String commitMessage;

    @Column(name = "branch", nullable = false, length = 200)
    private String branch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeploymentStatus status = DeploymentStatus.PENDING;

    /** Null when a CI key triggered the run; the label always carries something displayable. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by_id")
    private User triggeredBy;

    @Column(name = "triggered_by_label", nullable = false, length = 120)
    private String triggeredByLabel;

    @Column(name = "pipeline_url", length = 500)
    private String pipelineUrl;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt = Instant.now();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    protected Deployment() {
    }

    public Deployment(Project project, Environment environment, String releaseVersion,
                      String commitHash, String branch, User triggeredBy, String triggeredByLabel) {
        this.project = project;
        this.environment = environment;
        this.releaseVersion = releaseVersion;
        this.commitHash = commitHash;
        this.branch = branch;
        this.triggeredBy = triggeredBy;
        this.triggeredByLabel = triggeredByLabel;
    }

    /**
     * The only way the status changes. Timestamps are stamped here so that startedAt, finishedAt
     * and durationSeconds cannot drift apart from the status, and so the schema's own checks
     * (a finish implies a start, and a finish is never before it) hold by construction.
     */
    public void transitionTo(DeploymentStatus next, String reason) {
        if (!status.canTransitionTo(next)) {
            throw new BusinessRuleException(status.isTerminal()
                    ? "This deployment already finished as %s".formatted(status)
                    : "A %s deployment cannot move to %s".formatted(status, next));
        }

        if (next == DeploymentStatus.RUNNING) {
            this.startedAt = Instant.now();
        }
        if (next.isFinished() && startedAt != null) {
            this.finishedAt = Instant.now();
            this.durationSeconds = (int) Duration.between(startedAt, finishedAt).toSeconds();
        }
        // A cancellation keeps its reason too: a pipeline that gives up waiting for approval is
        // the commonest way a release ends CANCELLED, and "why" is the only useful thing to know
        // about it afterwards.
        if (next == DeploymentStatus.FAILED || next == DeploymentStatus.CANCELLED) {
            this.failureReason = reason;
        }

        this.status = next;
    }

    public Project getProject() {
        return project;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getCommitMessage() {
        return commitMessage;
    }

    public void setCommitMessage(String commitMessage) {
        this.commitMessage = commitMessage;
    }

    public String getBranch() {
        return branch;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public User getTriggeredBy() {
        return triggeredBy;
    }

    public String getTriggeredByLabel() {
        return triggeredByLabel;
    }

    public String getPipelineUrl() {
        return pipelineUrl;
    }

    public void setPipelineUrl(String pipelineUrl) {
        this.pipelineUrl = pipelineUrl;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }
}
