package com.devflow.deployment.entity;

import com.devflow.common.AuditedEntity;
import com.devflow.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "environments")
public class Environment extends AuditedEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private EnvironmentType type;

    @Column(name = "url", length = 500)
    private String url;

    /**
     * Gates automated releases: a deployment a CI key creates for this environment stays PENDING
     * until a project administrator starts it. Production is the environment this exists for.
     */
    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    protected Environment() {
    }

    public Environment(Project project, String name, EnvironmentType type) {
        this.project = project;
        this.name = name;
        this.type = type;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EnvironmentType getType() {
        return type;
    }

    public void setType(EnvironmentType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }
}
