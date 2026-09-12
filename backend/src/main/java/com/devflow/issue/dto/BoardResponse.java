package com.devflow.issue.dto;

import com.devflow.issue.entity.IssueStatus;
import com.devflow.project.dto.ProjectSummary;

import java.util.List;

public record BoardResponse(
        ProjectSummary project,
        List<BoardColumn> columns) {

    /**
     * {@code total} is the size of the whole column, while {@code issues} is capped. A column with
     * thousands of finished issues must not turn the board into an unbounded response.
     */
    public record BoardColumn(
            IssueStatus status,
            long total,
            List<IssueSummary> issues) {
    }
}
