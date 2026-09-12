package com.devflow.audit.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.entity.AuditLog;
import com.devflow.audit.repository.AuditLogRepository;
import com.devflow.security.CurrentUser;
import com.devflow.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(AuditAction action, String entityType, UUID entityId, UUID projectId, String summary) {
        record(action, entityType, entityId, projectId, summary, Map.of());
    }

    public void record(AuditAction action, String entityType, UUID entityId, UUID projectId,
                       String summary, Map<String, Object> metadata) {
        UserPrincipal actor = CurrentUser.principalOrNull();
        recordAs(actor == null ? null : actor.getId(),
                actor == null ? "system" : actor.getUsername(),
                action, entityType, entityId, projectId, summary, metadata);
    }

    /**
     * Deliberately joins the caller's transaction rather than starting its own. A separate
     * transaction runs on a second connection and cannot see rows the caller has not committed,
     * so auditing a just-created entity would fail its actor/project foreign keys. Sharing the
     * transaction also means a rolled-back operation leaves no misleading log entry behind.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordAs(UUID actorId, String actorLabel, AuditAction action, String entityType,
                         UUID entityId, UUID projectId, String summary, Map<String, Object> metadata) {
        AuditLog entry = new AuditLog(actorId, actorLabel, action, entityType, entityId, projectId, summary, metadata);
        entry.setIpAddress(currentIpAddress());
        auditLogRepository.save(entry);
    }

    private String currentIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
