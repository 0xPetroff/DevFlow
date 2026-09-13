package com.devflow.audit.entity;

import com.devflow.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

/**
 * Append-only record of significant actions. Actor and project are stored as raw ids rather
 * than associations so a log entry survives its subject being deleted and never drags an
 * object graph into a listing query.
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_label", nullable = false, length = 120)
    private String actorLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    @Column(name = "entity_type", nullable = false, length = 40)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", nullable = false)
    private Map<String, Object> metadata = Map.of();

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    protected AuditLog() {
    }

    public AuditLog(UUID actorId, String actorLabel, AuditAction action, String entityType,
                    UUID entityId, UUID projectId, String summary, Map<String, Object> metadata) {
        this.actorId = actorId;
        this.actorLabel = actorLabel;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.projectId = projectId;
        this.summary = summary.length() <= 500 ? summary : summary.substring(0, 500);
        this.metadata = metadata == null ? Map.of() : metadata;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActorLabel() {
        return actorLabel;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getSummary() {
        return summary;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
