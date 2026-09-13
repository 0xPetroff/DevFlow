package com.devflow.dashboard.dto;

import com.devflow.deployment.dto.DeploymentResponse;
import com.devflow.deployment.entity.DeploymentStatus;
import com.devflow.issue.entity.IssueStatus;

import java.util.List;
import java.util.Map;

/** Everything the landing page needs, scoped to the projects the caller can see. */
public record DashboardResponse(
        Projects projects,
        Issues issues,
        Deployments deployments) {

    public record Projects(long total, long active, long archived) {
    }

    public record Issues(
            long total,
            Map<IssueStatus, Long> byStatus,
            long assignedToMe,
            long overdue) {
    }

    public record Deployments(
            int windowDays,
            long total,
            Map<DeploymentStatus, Long> byStatus,
            // Null rather than zero when nothing finished in the window: no runs is not a 0% rate.
            Double successRate,
            List<DeploymentResponse> recent) {
    }
}
