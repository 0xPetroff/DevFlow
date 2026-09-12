package com.devflow.issue.dto;

import com.devflow.issue.entity.IssuePriority;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.IssueType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The optional query parameters of an issue listing, gathered so they travel as one argument. */
public record IssueFilter(
        List<IssueStatus> statuses,
        List<IssuePriority> priorities,
        List<IssueType> types,
        UUID assigneeId,
        Boolean unassigned,
        UUID labelId,
        LocalDate dueBefore,
        String search) {
}
