package com.devflow.issue.dto;

import com.devflow.issue.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Where a dragged card landed, described by its new neighbours rather than by a number. The
 * server derives the position from them, so two clients dragging at once cannot invent
 * conflicting orderings, and the board never has to renumber every card it did not touch.
 * Both neighbours are null when the card is dropped into an empty column.
 */
public record MoveIssueRequest(
        @NotNull IssueStatus status,
        UUID previousIssueId,
        UUID nextIssueId) {
}
