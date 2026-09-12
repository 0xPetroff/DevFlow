package com.devflow.issue.dto;

import com.devflow.issue.entity.IssuePriority;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.IssueType;
import com.devflow.project.dto.ProjectSummary;
import com.devflow.user.dto.UserSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        String key,
        int issueNumber,
        ProjectSummary project,
        String title,
        String description,
        IssueStatus status,
        IssuePriority priority,
        IssueType type,
        UserSummary assignee,
        UserSummary creator,
        LocalDate dueDate,
        double boardPosition,
        Short estimatePoints,
        List<LabelResponse> labels,
        Instant createdAt,
        Instant updatedAt) {
}
