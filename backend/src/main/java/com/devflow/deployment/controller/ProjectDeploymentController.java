package com.devflow.deployment.controller;

import com.devflow.common.PageResponse;
import com.devflow.common.SortProperties;
import com.devflow.deployment.dto.DeploymentResponse;
import com.devflow.deployment.entity.DeploymentStatus;
import com.devflow.deployment.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/deployments")
@Tag(name = "Deployments", description = "Release history and the deployment state machine")
public class ProjectDeploymentController {

    private static final Set<String> SORTABLE = Set.of("queuedAt", "startedAt", "finishedAt",
            "status", "releaseVersion", "branch", "durationSeconds");

    private final DeploymentService deploymentService;

    public ProjectDeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @GetMapping
    @PreAuthorize("@projectAccess.canRead(#projectId)")
    @Operation(summary = "List a project's deployments, newest first")
    public PageResponse<DeploymentResponse> list(@PathVariable UUID projectId,
                                                 @RequestParam(required = false) List<DeploymentStatus> status,
                                                 @RequestParam(required = false) UUID environmentId,
                                                 @RequestParam(required = false) String branch,
                                                 @RequestParam(required = false) String q,
                                                 @PageableDefault(size = 25, sort = "queuedAt",
                                                         direction = Sort.Direction.DESC) Pageable pageable) {
        return deploymentService.search(projectId, status, environmentId, branch, q,
                SortProperties.validate(pageable, SORTABLE));
    }
}
