package com.devflow.project.entity;

import com.devflow.common.AuditedEntity;
import com.devflow.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Locale;

@Entity
@Table(name = "projects")
public class Project extends AuditedEntity {

    @Column(name = "project_key", nullable = false, length = 10, updatable = false)
    private String projectKey;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Backs the per-project DEVF-42 issue keys. Never incremented through this field: Stage 4
     * bumps it with an atomic UPDATE ... RETURNING so two concurrent creates cannot collide.
     */
    @Column(name = "issue_sequence", nullable = false, insertable = false, updatable = false)
    private int issueSequence;

    protected Project() {
    }

    public Project(String projectKey, String name, User owner) {
        this.projectKey = normaliseKey(projectKey);
        this.name = name;
        this.owner = owner;
    }

    /** Keys are stored upper case because the schema's format check and the issue keys both assume it. */
    public static String normaliseKey(String key) {
        return key == null ? null : key.trim().toUpperCase(Locale.ROOT);
    }

    public String getProjectKey() {
        return projectKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public int getIssueSequence() {
        return issueSequence;
    }

    public boolean isArchived() {
        return status == ProjectStatus.ARCHIVED;
    }
}
