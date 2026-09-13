package com.devflow.audit.dto;

import com.devflow.audit.entity.AuditAction;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        String actorLabel,
        AuditAction action,
        String entityType,
        UUID entityId,
        UUID projectId,
        String summary,
        Map<String, Object> metadata,
        Instant createdAt) {
}
