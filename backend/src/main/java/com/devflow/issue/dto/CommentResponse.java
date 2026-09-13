package com.devflow.issue.dto;

import com.devflow.user.dto.UserSummary;

import java.time.Instant;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UserSummary author,
        String body,
        boolean edited,
        Instant createdAt,
        Instant updatedAt) {
}
