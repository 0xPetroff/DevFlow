package com.devflow.project.dto;

import com.devflow.user.dto.UserSummary;
import com.devflow.user.entity.Role;

import java.time.Instant;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID id,
        UserSummary user,
        Role role,
        boolean owner,
        Instant createdAt) {
}
