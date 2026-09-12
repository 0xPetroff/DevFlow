package com.devflow.issue.dto;

import com.devflow.issue.entity.IssuePriority;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.IssueType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A full replacement of the editable fields: an omitted assignee, due date or label list clears
 * that field rather than leaving it alone. Board position is not here; it moves through
 * PUT /api/issues/{id}/position so a drag and an edit cannot overwrite each other.
 */
public record UpdateIssueRequest(
        @NotBlank @Size(max = 200) String title,
        String description,
        @NotNull IssueStatus status,
        @NotNull IssuePriority priority,
        @NotNull IssueType type,
        UUID assigneeId,
        LocalDate dueDate,
        @Min(0) @Max(100) Short estimatePoints,
        List<UUID> labelIds) {
}
