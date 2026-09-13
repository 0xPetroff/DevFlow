package com.devflow.issue.controller;

import com.devflow.issue.dto.IssueResponse;
import com.devflow.issue.dto.MoveIssueRequest;
import com.devflow.issue.dto.UpdateIssueRequest;
import com.devflow.issue.service.IssueService;
import com.devflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues")
@Tag(name = "Issues", description = "Issue tracking and the Kanban board")
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@issueAccess.canRead(#id)")
    @Operation(summary = "Fetch a single issue")
    public IssueResponse get(@PathVariable UUID id) {
        return issueService.getById(id);
    }

    @GetMapping("/key/{issueKey}")
    @Operation(summary = "Fetch an issue by its key, for example DEVF-42")
    public IssueResponse getByKey(@PathVariable String issueKey) {
        return issueService.getByKey(issueKey);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@issueAccess.canWrite(#id)")
    @Operation(summary = "Update an issue's editable fields")
    public IssueResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateIssueRequest request) {
        return issueService.update(id, request);
    }

    @PutMapping("/{id}/position")
    @PreAuthorize("@issueAccess.canWrite(#id)")
    @Operation(summary = "Move a card on the board, positioning it between its new neighbours")
    public IssueResponse move(@PathVariable UUID id, @Valid @RequestBody MoveIssueRequest request) {
        return issueService.move(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@issueAccess.canWrite(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an issue. Restricted to its reporter and project administrators.")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        issueService.delete(id, principal.getId());
    }
}
