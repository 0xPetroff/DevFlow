package com.devflow.issue.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.common.PageResponse;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.issue.dto.CommentRequest;
import com.devflow.issue.dto.CommentResponse;
import com.devflow.issue.entity.Comment;
import com.devflow.issue.entity.Issue;
import com.devflow.issue.mapper.CommentMapper;
import com.devflow.issue.repository.CommentRepository;
import com.devflow.project.service.ProjectAccessService;
import com.devflow.project.service.ProjectService;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final IssueService issueService;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccess;
    private final CommentMapper commentMapper;
    private final AuditService auditService;

    public CommentService(CommentRepository commentRepository,
                          UserRepository userRepository,
                          IssueService issueService,
                          ProjectService projectService,
                          ProjectAccessService projectAccess,
                          CommentMapper commentMapper,
                          AuditService auditService) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.issueService = issueService;
        this.projectService = projectService;
        this.projectAccess = projectAccess;
        this.commentMapper = commentMapper;
        this.auditService = auditService;
    }

    public PageResponse<CommentResponse> list(UUID issueId, Pageable pageable) {
        issueService.requireIssue(issueId);
        return PageResponse.from(commentRepository.findByIssueIdOrderByCreatedAtAsc(issueId, pageable)
                .map(commentMapper::toResponse));
    }

    @Transactional
    public CommentResponse create(UUID issueId, CommentRequest request, UUID actorId) {
        Issue issue = issueService.requireIssue(issueId);
        UUID projectId = issue.getProject().getId();
        projectService.requireActiveProject(projectId);

        User author = requireUser(actorId);
        Comment comment = commentRepository.save(new Comment(issue, author, request.body().trim()));

        auditService.record(AuditAction.COMMENT_CREATED, "Comment", comment.getId(), projectId,
                "Commented on %s".formatted(issue.getKey()), Map.of("issueKey", issue.getKey()));

        return commentMapper.toResponse(comment);
    }

    /** Only the author may rewrite their own words, whatever their role on the project. */
    @Transactional
    public CommentResponse update(UUID issueId, UUID commentId, CommentRequest request, UUID actorId) {
        Comment comment = requireComment(issueId, commentId);
        projectService.requireActiveProject(comment.getIssue().getProject().getId());

        if (!comment.getAuthor().getId().equals(actorId)) {
            throw new AccessDeniedException("Only the author can edit a comment");
        }

        comment.setBody(request.body().trim());
        // Flushed here so the auditing listener stamps updatedAt before the response is built;
        // without it the reply would still carry the pre-edit timestamp and read as unedited.
        return commentMapper.toResponse(commentRepository.saveAndFlush(comment));
    }

    @Transactional
    public void delete(UUID issueId, UUID commentId, UUID actorId) {
        Comment comment = requireComment(issueId, commentId);
        UUID projectId = comment.getIssue().getProject().getId();
        projectService.requireActiveProject(projectId);

        // Authors clear up after themselves; moderating anyone else's comment is an administrator's job.
        if (!comment.getAuthor().getId().equals(actorId) && !projectAccess.canAdmin(projectId)) {
            throw new AccessDeniedException("Only the author or a project administrator can delete a comment");
        }

        auditService.record(AuditAction.COMMENT_DELETED, "Comment", comment.getId(), projectId,
                "Deleted a comment on %s".formatted(comment.getIssue().getKey()),
                Map.of("issueKey", comment.getIssue().getKey(), "author", comment.getAuthor().getUsername()));

        commentRepository.delete(comment);
    }

    private Comment requireComment(UUID issueId, UUID commentId) {
        return commentRepository.findByIdAndIssueId(commentId, issueId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
