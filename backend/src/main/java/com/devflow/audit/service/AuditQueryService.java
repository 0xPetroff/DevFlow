package com.devflow.audit.service;

import com.devflow.audit.dto.AuditLogResponse;
import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.entity.AuditLog;
import com.devflow.audit.mapper.AuditLogMapper;
import com.devflow.audit.repository.AuditLogRepository;
import com.devflow.audit.repository.AuditLogSpecifications;
import com.devflow.common.PageResponse;
import com.devflow.project.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Reading the audit trail, kept apart from AuditService so that writing an entry stays a
 * transaction-joining side effect and reading one stays an ordinary query.
 *
 * <p>The response never carries the recorded IP address. It is kept for a forensic question asked
 * against the database, not for a listing every project member can open.
 */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;
    private final ProjectService projectService;

    public AuditQueryService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper,
                             ProjectService projectService) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
        this.projectService = projectService;
    }

    public PageResponse<AuditLogResponse> searchProject(UUID projectId, UUID actorId, AuditAction action,
                                                        Instant since, String search, Pageable pageable) {
        projectService.requireProject(projectId);
        return search(projectId, actorId, action, since, search, pageable);
    }

    /** Account-wide, for administrators: it also returns the entries no project owns. */
    public PageResponse<AuditLogResponse> searchAll(UUID actorId, AuditAction action, Instant since,
                                                    String search, Pageable pageable) {
        return search(null, actorId, action, since, search, pageable);
    }

    private PageResponse<AuditLogResponse> search(UUID projectId, UUID actorId, AuditAction action,
                                                  Instant since, String search, Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findAll(
                AuditLogSpecifications.matching(projectId, actorId, action, since, search), pageable);
        return PageResponse.from(page.map(auditLogMapper::toResponse));
    }
}
