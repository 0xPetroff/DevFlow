package com.devflow.issue.dto;

import com.devflow.issue.entity.IssuePriority;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.IssueType;
import com.devflow.user.dto.UserSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The board card: enough to render a column without the description or project envelope. */
public record IssueSummary(
        UUID id,
        String key,
        String title,
        IssueStatus status,
        IssuePriority priority,
        IssueType type,
        UserSummary assignee,
        LocalDate dueDate,
        double boardPosition,
        Short estimatePoints,
        List<LabelResponse> labels) {
}
