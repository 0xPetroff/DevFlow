package com.devflow.dashboard.service;

import com.devflow.dashboard.dto.DashboardResponse;
import com.devflow.deployment.dto.DeploymentResponse;
import com.devflow.deployment.entity.DeploymentStatus;
import com.devflow.deployment.mapper.DeploymentMapper;
import com.devflow.deployment.repository.DeploymentRepository;
import com.devflow.issue.entity.IssueStatus;
import com.devflow.issue.repository.IssueRepository;
import com.devflow.project.entity.ProjectStatus;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.security.UserPrincipal;
import com.devflow.user.entity.Role;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECENT_DEPLOYMENTS = 5;

    private final ProjectRepository projectRepository;
    private final IssueRepository issueRepository;
    private final DeploymentRepository deploymentRepository;
    private final DeploymentMapper deploymentMapper;

    public DashboardService(ProjectRepository projectRepository,
                            IssueRepository issueRepository,
                            DeploymentRepository deploymentRepository,
                            DeploymentMapper deploymentMapper) {
        this.projectRepository = projectRepository;
        this.issueRepository = issueRepository;
        this.deploymentRepository = deploymentRepository;
        this.deploymentMapper = deploymentMapper;
    }

    public DashboardResponse forUser(UserPrincipal principal, int windowDays) {
        // Every figure below is bounded by this set, so the dashboard can never total up a project
        // the caller is not a member of.
        List<UUID> visible = principal.getRole() == Role.ADMIN
                ? projectRepository.findAllIds()
                : projectRepository.findVisibleIds(principal.getId());

        if (visible.isEmpty()) {
            return empty(windowDays);
        }

        return new DashboardResponse(
                projects(visible),
                issues(visible, principal.getId()),
                deployments(visible, windowDays));
    }

    private DashboardResponse.Projects projects(List<UUID> visible) {
        long active = projectRepository.countByIdInAndStatus(visible, ProjectStatus.ACTIVE);
        return new DashboardResponse.Projects(visible.size(), active, visible.size() - active);
    }

    private DashboardResponse.Issues issues(List<UUID> visible, UUID userId) {
        Map<IssueStatus, Long> byStatus = new EnumMap<>(IssueStatus.class);
        for (IssueStatus status : IssueStatus.values()) {
            byStatus.put(status, 0L);
        }
        issueRepository.countByStatus(visible)
                .forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long overdue = issueRepository.countByProjectIdInAndDueDateBeforeAndStatusNot(
                visible, LocalDate.now(), IssueStatus.DONE);

        return new DashboardResponse.Issues(total, byStatus,
                issueRepository.countByProjectIdInAndAssigneeId(visible, userId), overdue);
    }

    private DashboardResponse.Deployments deployments(List<UUID> visible, int windowDays) {
        Instant since = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        Map<DeploymentStatus, Long> byStatus = new EnumMap<>(DeploymentStatus.class);
        for (DeploymentStatus status : DeploymentStatus.values()) {
            byStatus.put(status, 0L);
        }
        deploymentRepository.countByStatusSince(visible, since)
                .forEach(row -> byStatus.put(row.getStatus(), row.getTotal()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        List<DeploymentStatus> conclusive = List.of(DeploymentStatus.SUCCESS, DeploymentStatus.FAILED);
        long decided = conclusive.stream().mapToLong(byStatus::get).sum();

        // Runs still in flight, and ones that were cancelled, say nothing about reliability.
        Double successRate = decided == 0 ? null : (double) byStatus.get(DeploymentStatus.SUCCESS) / decided;

        List<DeploymentResponse> recent = deploymentRepository
                .findByProjectIdInOrderByQueuedAtDesc(visible, PageRequest.of(0, RECENT_DEPLOYMENTS))
                .stream().map(deploymentMapper::toResponse).toList();

        return new DashboardResponse.Deployments(windowDays, total, byStatus, successRate, recent);
    }

    private DashboardResponse empty(int windowDays) {
        Map<IssueStatus, Long> issueStatuses = new EnumMap<>(IssueStatus.class);
        for (IssueStatus status : IssueStatus.values()) {
            issueStatuses.put(status, 0L);
        }
        Map<DeploymentStatus, Long> deploymentStatuses = new EnumMap<>(DeploymentStatus.class);
        for (DeploymentStatus status : DeploymentStatus.values()) {
            deploymentStatuses.put(status, 0L);
        }

        return new DashboardResponse(
                new DashboardResponse.Projects(0, 0, 0),
                new DashboardResponse.Issues(0, issueStatuses, 0, 0),
                new DashboardResponse.Deployments(windowDays, 0, deploymentStatuses, null, List.of()));
    }
}
