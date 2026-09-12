package com.devflow.audit.controller;

import com.devflow.audit.dto.AuditLogResponse;
import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditQueryService;
import com.devflow.common.PageResponse;
import com.devflow.common.SortProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@RestController
@Tag(name = "Audit", description = "The append-only record of who changed what")
public class AuditLogController {

    private static final Set<String> SORTABLE = Set.of("createdAt", "action", "actorLabel");

    private final AuditQueryService auditQueryService;

    public AuditLogController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping("/api/projects/{projectId}/audit-logs")
    @PreAuthorize("@projectAccess.canRead(#projectId)")
    @Operation(summary = "The activity recorded against one project, newest first")
    public PageResponse<AuditLogResponse> listForProject(@PathVariable UUID projectId,
                                                         @RequestParam(required = false) UUID actorId,
                                                         @RequestParam(required = false) AuditAction action,
                                                         @RequestParam(required = false)
                                                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                                                         Instant since,
                                                         @RequestParam(required = false) String q,
                                                         @PageableDefault(size = 25, sort = "createdAt",
                                                                 direction = Sort.Direction.DESC) Pageable pageable) {
        return auditQueryService.searchProject(projectId, actorId, action, since, q,
                SortProperties.validate(pageable, SORTABLE));
    }

    @GetMapping("/api/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Every recorded action across the installation, newest first")
    public PageResponse<AuditLogResponse> list(@RequestParam(required = false) UUID actorId,
                                               @RequestParam(required = false) AuditAction action,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since,
                                               @RequestParam(required = false) String q,
                                               @PageableDefault(size = 25, sort = "createdAt",
                                                       direction = Sort.Direction.DESC) Pageable pageable) {
        return auditQueryService.searchAll(actorId, action, since, q,
                SortProperties.validate(pageable, SORTABLE));
    }
}
