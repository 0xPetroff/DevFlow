package com.devflow.issue.dto;

import com.devflow.issue.entity.IssuePriority;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Every field but the title is optional; the entity defaults cover TODO, MEDIUM and TASK. */
public record CreateIssueRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        IssueStatus status,
        IssuePriority priority,
        IssueType type,
        UUID assigneeId,
        LocalDate dueDate,
        @Min(0) @Max(100) Short estimatePoints,
        List<UUID> labelIds) {
}
