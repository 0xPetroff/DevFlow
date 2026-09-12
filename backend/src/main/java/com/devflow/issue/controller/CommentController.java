package com.devflow.issue.controller;

import com.devflow.common.PageResponse;
import com.devflow.issue.dto.CommentRequest;
import com.devflow.issue.dto.CommentResponse;
import com.devflow.issue.service.CommentService;
import com.devflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/issues/{issueId}/comments")
@Tag(name = "Comments", description = "Discussion on an issue")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    @PreAuthorize("@issueAccess.canRead(#issueId)")
    @Operation(summary = "List an issue's comments, oldest first")
    public PageResponse<CommentResponse> list(@PathVariable UUID issueId,
                                              @PageableDefault(size = 50) Pageable pageable) {
        return commentService.list(issueId, pageable);
    }

    @PostMapping
    @PreAuthorize("@issueAccess.canWrite(#issueId)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comment on an issue")
    public CommentResponse create(@PathVariable UUID issueId,
                                  @Valid @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.create(issueId, request, principal.getId());
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("@issueAccess.canWrite(#issueId)")
    @Operation(summary = "Edit your own comment")
    public CommentResponse update(@PathVariable UUID issueId, @PathVariable UUID commentId,
                                  @Valid @RequestBody CommentRequest request,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        return commentService.update(issueId, commentId, request, principal.getId());
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("@issueAccess.canWrite(#issueId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment. Restricted to its author and project administrators.")
    public void delete(@PathVariable UUID issueId, @PathVariable UUID commentId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        commentService.delete(issueId, commentId, principal.getId());
    }
}
