package com.devflow.issue.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.common.PageResponse;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.issue.dto.BoardResponse;
import com.devflow.issue.dto.CreateIssueRequest;
import com.devflow.issue.dto.IssueFilter;
import com.devflow.issue.dto.IssueResponse;
import com.devflow.issue.dto.MoveIssueRequest;
import com.devflow.issue.dto.UpdateIssueRequest;
import com.devflow.issue.entity.Issue;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.entity.Label;
import com.devflow.issue.mapper.IssueMapper;
import com.devflow.issue.repository.IssueRepository;
import com.devflow.issue.repository.IssueSpecifications;
import com.devflow.issue.repository.LabelRepository;
import com.devflow.project.entity.Project;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.ProjectMemberRepository;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.project.service.ProjectAccessService;
import com.devflow.project.service.ProjectService;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@Transactional(readOnly = true)
public class IssueService {

    /** Below this gap the doubles are too close to split again, so the column is renumbered. */
    private static final double MIN_POSITION_GAP = 0.0001;

    private final IssueRepository issueRepository;
    private final LabelRepository labelRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccess;
    private final IssueMapper issueMapper;
    private final ProjectMapper projectMapper;
    private final AuditService auditService;

    public IssueService(IssueRepository issueRepository,
                        LabelRepository labelRepository,
                        ProjectRepository projectRepository,
                        ProjectMemberRepository memberRepository,
                        UserRepository userRepository,
                        ProjectService projectService,
                        ProjectAccessService projectAccess,
                        IssueMapper issueMapper,
                        ProjectMapper projectMapper,
                        AuditService auditService) {
        this.issueRepository = issueRepository;
        this.labelRepository = labelRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.projectAccess = projectAccess;
        this.issueMapper = issueMapper;
        this.projectMapper = projectMapper;
        this.auditService = auditService;
    }

    @Transactional
    public IssueResponse create(UUID projectId, CreateIssueRequest request, UUID actorId) {
        Project project = projectService.requireActiveProject(projectId);
        User creator = requireUser(actorId);

        Issue issue = new Issue(project, projectRepository.claimNextIssueNumber(projectId),
                request.title().trim(), creator);
        issue.setDescription(trimToNull(request.description()));
        applyIfPresent(request.status(), issue::setStatus);
        applyIfPresent(request.priority(), issue::setPriority);
        applyIfPresent(request.type(), issue::setType);
        issue.setDueDate(request.dueDate());
        issue.setEstimatePoints(request.estimatePoints());
        issue.setAssignee(resolveAssignee(projectId, request.assigneeId()));
        issue.replaceLabels(resolveLabels(projectId, request.labelIds()));
        issue.setBoardPosition(endOfColumn(projectId, issue.getStatus()));

        issueRepository.save(issue);

        auditService.record(AuditAction.ISSUE_CREATED, "Issue", issue.getId(), projectId,
                "Created %s: %s".formatted(issue.getKey(), issue.getTitle()),
                Map.of("key", issue.getKey(), "status", issue.getStatus().name()));

        return issueMapper.toResponse(issue);
    }

    public IssueResponse getById(UUID id) {
        return issueMapper.toResponse(requireIssue(id));
    }

    /**
     * Unknown and unreadable keys both fail as denied, matching project lookup: a 404 here would
     * report whether DEVF-41 exists to someone with no access to DEVF.
     */
    public IssueResponse getByKey(String issueKey) {
        int separator = issueKey.lastIndexOf('-');
        if (separator <= 0) {
            throw new AccessDeniedException("No readable issue with this key");
        }

        Issue issue = projectRepository.findByProjectKey(Project.normaliseKey(issueKey.substring(0, separator)))
                .flatMap(project -> issueRepository.findByProjectIdAndIssueNumber(
                        project.getId(), parseIssueNumber(issueKey.substring(separator + 1))))
                .filter(candidate -> projectAccess.canRead(candidate.getProject().getId()))
                .orElseThrow(() -> new AccessDeniedException("No readable issue with this key"));

        return issueMapper.toResponse(issue);
    }

    public PageResponse<IssueResponse> search(UUID projectId, IssueFilter filter, Pageable pageable) {
        return PageResponse.from(issueRepository
                .findAll(IssueSpecifications.matching(projectId, filter), pageable)
                .map(issueMapper::toResponse));
    }

    public BoardResponse board(UUID projectId, int limitPerColumn) {
        Project project = projectService.requireProject(projectId);
        Pageable cap = PageRequest.of(0, limitPerColumn);

        List<BoardResponse.BoardColumn> columns = Arrays.stream(IssueStatus.values())
                .map(status -> new BoardResponse.BoardColumn(status,
                        issueRepository.countByProjectIdAndStatus(projectId, status),
                        issueRepository.findByProjectIdAndStatusOrderByBoardPositionAsc(projectId, status, cap)
                                .stream().map(issueMapper::toSummary).toList()))
                .toList();

        return new BoardResponse(projectMapper.toSummary(project), columns);
    }

    @Transactional
    public IssueResponse update(UUID id, UpdateIssueRequest request) {
        Issue issue = requireIssue(id);
        UUID projectId = issue.getProject().getId();
        projectService.requireActiveProject(projectId);

        IssueStatus previousStatus = issue.getStatus();
        issue.setTitle(request.title().trim());
        issue.setDescription(trimToNull(request.description()));
        issue.setStatus(request.status());
        issue.setPriority(request.priority());
        issue.setType(request.type());
        issue.setDueDate(request.dueDate());
        issue.setEstimatePoints(request.estimatePoints());
        issue.setAssignee(resolveAssignee(projectId, request.assigneeId()));
        issue.replaceLabels(resolveLabels(projectId, request.labelIds()));

        // Editing the status outside the board leaves the old position meaningless in the new column.
        if (previousStatus != request.status()) {
            issue.setBoardPosition(endOfColumn(projectId, request.status()));
        }

        Map<String, Object> metadata = new HashMap<>(Map.of("key", issue.getKey()));
        if (previousStatus != request.status()) {
            metadata.put("statusFrom", previousStatus.name());
            metadata.put("statusTo", request.status().name());
        }

        auditService.record(AuditAction.ISSUE_UPDATED, "Issue", issue.getId(), projectId,
                "Updated %s".formatted(issue.getKey()), metadata);

        return issueMapper.toResponse(issue);
    }

    /**
     * Positions the card between the neighbours the client dropped it against. Splitting the gap
     * keeps every other card in the column untouched, which is what makes a drag one UPDATE.
     */
    @Transactional
    public IssueResponse move(UUID id, MoveIssueRequest request) {
        Issue issue = requireIssue(id);
        UUID projectId = issue.getProject().getId();
        projectService.requireActiveProject(projectId);

        Issue previous = resolveNeighbour(request.previousIssueId(), id, projectId, request.status());
        Issue next = resolveNeighbour(request.nextIssueId(), id, projectId, request.status());
        if (previous != null && next != null && previous.getBoardPosition() >= next.getBoardPosition()) {
            throw new BusinessRuleException("The neighbouring cards are not in board order");
        }

        // Renumber before splitting rather than after: once the gap is unsplittable the midpoint
        // would land on a neighbour's position and the resulting order would be arbitrary.
        if (previous != null && next != null
                && next.getBoardPosition() - previous.getBoardPosition() < MIN_POSITION_GAP) {
            rebalance(projectId, request.status());
        }

        // Worked out before the status changes: once the card belongs to the target column, a drop
        // with no neighbours would measure the column against the card's own old position.
        double position = positionBetween(previous, next, projectId, request.status());

        IssueStatus previousStatus = issue.getStatus();
        issue.setStatus(request.status());
        issue.setBoardPosition(position);

        if (previousStatus != request.status()) {
            auditService.record(AuditAction.ISSUE_UPDATED, "Issue", issue.getId(), projectId,
                    "Moved %s from %s to %s".formatted(issue.getKey(), previousStatus, request.status()),
                    Map.of("key", issue.getKey(), "statusFrom", previousStatus.name(),
                            "statusTo", request.status().name()));
        }

        return issueMapper.toResponse(issue);
    }

    @Transactional
    public void delete(UUID id, UUID actorId) {
        Issue issue = requireIssue(id);
        UUID projectId = issue.getProject().getId();
        projectService.requireActiveProject(projectId);

        // Writing gets you your own issues; clearing up someone else's is an administrator's job.
        if (!issue.getCreator().getId().equals(actorId) && !projectAccess.canAdmin(projectId)) {
            throw new AccessDeniedException("Only the reporter or a project administrator can delete an issue");
        }

        auditService.record(AuditAction.ISSUE_DELETED, "Issue", issue.getId(), projectId,
                "Deleted %s: %s".formatted(issue.getKey(), issue.getTitle()),
                Map.of("key", issue.getKey()));

        issueRepository.delete(issue);
    }

    public Issue requireIssue(UUID id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Issue", id));
    }

    private double positionBetween(Issue previous, Issue next, UUID projectId, IssueStatus status) {
        if (previous == null && next == null) {
            return endOfColumn(projectId, status);
        }
        if (previous == null) {
            return next.getBoardPosition() / 2;
        }
        if (next == null) {
            return previous.getBoardPosition() + Issue.POSITION_GAP;
        }
        return (previous.getBoardPosition() + next.getBoardPosition()) / 2;
    }

    private void rebalance(UUID projectId, IssueStatus status) {
        List<Issue> column = issueRepository.findByProjectIdAndStatusOrderByBoardPositionAsc(projectId, status);
        for (int index = 0; index < column.size(); index++) {
            column.get(index).setBoardPosition((index + 1) * Issue.POSITION_GAP);
        }
    }

    private double endOfColumn(UUID projectId, IssueStatus status) {
        return issueRepository.findLastPosition(projectId, status)
                .map(last -> last + Issue.POSITION_GAP)
                .orElse(Issue.POSITION_GAP);
    }

    private Issue resolveNeighbour(UUID neighbourId, UUID movedIssueId, UUID projectId, IssueStatus status) {
        if (neighbourId == null) {
            return null;
        }
        if (neighbourId.equals(movedIssueId)) {
            throw new BusinessRuleException("An issue cannot be positioned against itself");
        }
        Issue neighbour = requireIssue(neighbourId);
        if (!neighbour.getProject().getId().equals(projectId) || neighbour.getStatus() != status) {
            throw new BusinessRuleException("The neighbouring cards must be in the target column");
        }
        return neighbour;
    }

    private User resolveAssignee(UUID projectId, UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        User assignee = requireUser(assigneeId);
        if (!memberRepository.existsByProjectIdAndUserId(projectId, assigneeId)) {
            throw new BusinessRuleException("The assignee must be a member of the project");
        }
        return assignee;
    }

    private Set<Label> resolveLabels(UUID projectId, List<UUID> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) {
            return Set.of();
        }
        List<UUID> wanted = new ArrayList<>(new LinkedHashSet<>(labelIds));
        List<Label> found = labelRepository.findByProjectIdAndIdIn(projectId, wanted);
        if (found.size() != wanted.size()) {
            throw new BusinessRuleException("One or more labels do not belong to this project");
        }
        return new LinkedHashSet<>(found);
    }

    private int parseIssueNumber(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new AccessDeniedException("No readable issue with this key");
        }
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private static <T> void applyIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
