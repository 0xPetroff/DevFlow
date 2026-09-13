package com.devflow.audit.mapper;

import com.devflow.audit.dto.AuditLogResponse;
import com.devflow.audit.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);
}
