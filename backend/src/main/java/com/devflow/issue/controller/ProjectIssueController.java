package com.devflow.issue.controller;

import com.devflow.common.PageResponse;
import com.devflow.common.SortProperties;
import com.devflow.issue.dto.BoardResponse;
import com.devflow.issue.dto.CreateIssueRequest;
import com.devflow.issue.dto.IssueFilter;
import com.devflow.issue.dto.IssueResponse;
import com.devflow.issue.entity.IssuePriority;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.IssueType;
import com.devflow.issue.service.IssueService;
import com.devflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}")
@Tag(name = "Issues", description = "Issue tracking and the Kanban board")
public class ProjectIssueController {

    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "issueNumber",
            "title", "status", "priority", "type", "dueDate", "boardPosition", "estimatePoints");

    private static final int MAX_BOARD_COLUMN_SIZE = 200;

    private final IssueService issueService;

    public ProjectIssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping("/issues")
    @PreAuthorize("@projectAccess.canWrite(#projectId)")
    @Operation(summary = "Create an issue, numbered DEVF-42 style within its project")
    public ResponseEntity<IssueResponse> create(@PathVariable UUID projectId,
                                                @Valid @RequestBody CreateIssueRequest request,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        IssueResponse created = issueService.create(projectId, request, principal.getId());
        return ResponseEntity.created(URI.create("/api/issues/" + created.id())).body(created);
    }

    @GetMapping("/issues")
    @PreAuthorize("@projectAccess.canRead(#projectId)")
    @Operation(summary = "List a project's issues, filtered, sorted and paginated")
    public PageResponse<IssueResponse> list(@PathVariable UUID projectId,
                                            @RequestParam(required = false) List<IssueStatus> status,
                                            @RequestParam(required = false) List<IssuePriority> priority,
                                            @RequestParam(required = false) List<IssueType> type,
                                            @RequestParam(required = false) UUID assigneeId,
                                            @RequestParam(required = false) Boolean unassigned,
                                            @RequestParam(required = false) UUID labelId,
                                            @RequestParam(required = false)
                                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueBefore,
                                            @RequestParam(required = false) String q,
                                            @PageableDefault(size = 25, sort = "createdAt",
                                                    direction = Sort.Direction.DESC) Pageable pageable) {
        IssueFilter filter = new IssueFilter(status, priority, type, assigneeId, unassigned, labelId, dueBefore, q);
        return issueService.search(projectId, filter, SortProperties.validate(pageable, SORTABLE));
    }

    @GetMapping("/board")
    @PreAuthorize("@projectAccess.canRead(#projectId)")
    @Operation(summary = "The Kanban board: every column, ordered by board position")
    public BoardResponse board(@PathVariable UUID projectId,
                               @RequestParam(defaultValue = "100") int limitPerColumn) {
        return issueService.board(projectId, Math.clamp(limitPerColumn, 1, MAX_BOARD_COLUMN_SIZE));
    }
}
