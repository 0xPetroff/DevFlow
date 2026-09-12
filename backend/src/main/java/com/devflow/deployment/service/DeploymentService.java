package com.devflow.deployment.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.common.PageResponse;
import com.devflow.deployment.dto.CreateDeploymentRequest;
import com.devflow.deployment.dto.DeploymentResponse;
import com.devflow.deployment.dto.DeploymentStatusRequest;
import com.devflow.deployment.entity.Deployment;
import com.devflow.deployment.entity.DeploymentStatus;
import com.devflow.deployment.entity.Environment;
import com.devflow.deployment.mapper.DeploymentMapper;
import com.devflow.deployment.repository.DeploymentRepository;
import com.devflow.deployment.repository.DeploymentSpecifications;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.project.entity.Project;
import com.devflow.project.service.ProjectAccessService;
import com.devflow.project.service.ProjectService;
import com.devflow.security.ApiKeyPrincipal;
import com.devflow.security.CurrentApiKey;
import com.devflow.security.CurrentUser;
import com.devflow.security.UserPrincipal;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;
    private final UserRepository userRepository;
    private final EnvironmentService environmentService;
    private final ProjectService projectService;
    private final ProjectAccessService projectAccess;
    private final DeploymentMapper deploymentMapper;
    private final AuditService auditService;

    public DeploymentService(DeploymentRepository deploymentRepository,
                             UserRepository userRepository,
                             EnvironmentService environmentService,
                             ProjectService projectService,
                             ProjectAccessService projectAccess,
                             DeploymentMapper deploymentMapper,
                             AuditService auditService) {
        this.deploymentRepository = deploymentRepository;
        this.userRepository = userRepository;
        this.environmentService = environmentService;
        this.projectService = projectService;
        this.projectAccess = projectAccess;
        this.deploymentMapper = deploymentMapper;
        this.auditService = auditService;
    }

    @Transactional
    public DeploymentResponse create(CreateDeploymentRequest request) {
        Project project = projectService.requireActiveProject(request.projectId());
        Environment environment = environmentService.requireByName(project.getId(), request.environmentName());
        Actor actor = currentActor();

        Deployment deployment = new Deployment(project, environment, request.releaseVersion().trim(),
                request.commitHash().toLowerCase(Locale.ROOT), request.branch().trim(),
                actor.user(), actor.label());
        deployment.setCommitMessage(trimToNull(request.commitMessage()));
        deployment.setPipelineUrl(trimToNull(request.pipelineUrl()));
        deploymentRepository.save(deployment);

        auditService.recordAs(actor.id(), actor.label(), AuditAction.DEPLOYMENT_CREATED, "Deployment",
                deployment.getId(), project.getId(),
                "Queued %s to %s".formatted(deployment.getReleaseVersion(), environment.getName()),
                Map.of("environment", environment.getName(), "branch", deployment.getBranch(),
                        "commit", deployment.getCommitHash()));

        return deploymentMapper.toResponse(deployment);
    }

    public DeploymentResponse getById(UUID id) {
        return deploymentMapper.toResponse(requireDeployment(id));
    }

    public PageResponse<DeploymentResponse> search(UUID projectId, List<DeploymentStatus> statuses,
                                                   UUID environmentId, String branch, String search,
                                                   Pageable pageable) {
        return PageResponse.from(deploymentRepository
                .findAll(DeploymentSpecifications.matching(projectId, statuses, environmentId, branch, search),
                        pageable)
                .map(deploymentMapper::toResponse));
    }

    /**
     * The single door into the state machine. The entity decides whether the move is legal; what
     * this adds is the approval gate, which is the whole point of Environment.requiresApproval:
     * a run against such an environment is queued by CI but only ever started by a person who
     * administers the project.
     */
    @Transactional
    public DeploymentResponse transition(UUID id, DeploymentStatusRequest request) {
        Deployment deployment = requireDeployment(id);
        UUID projectId = deployment.getProject().getId();
        projectService.requireActiveProject(projectId);

        if (request.status() == DeploymentStatus.RUNNING
                && deployment.getEnvironment().isRequiresApproval()
                && !projectAccess.canAdmin(projectId)) {
            throw new AccessDeniedException(
                    "Environment %s requires a project administrator to approve the release"
                            .formatted(deployment.getEnvironment().getName()));
        }

        DeploymentStatus previous = deployment.getStatus();
        deployment.transitionTo(request.status(), trimToNull(request.failureReason()));

        Actor actor = currentActor();
        Map<String, Object> metadata = new HashMap<>(Map.of(
                "environment", deployment.getEnvironment().getName(),
                "statusFrom", previous.name(),
                "statusTo", request.status().name()));
        if (deployment.getDurationSeconds() != null) {
            metadata.put("durationSeconds", deployment.getDurationSeconds());
        }

        auditService.recordAs(actor.id(), actor.label(), auditActionFor(request.status()), "Deployment",
                deployment.getId(), projectId,
                "%s %s to %s".formatted(pastTense(request.status()), deployment.getReleaseVersion(),
                        deployment.getEnvironment().getName()),
                metadata);

        return deploymentMapper.toResponse(deployment);
    }

    private Deployment requireDeployment(UUID id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Deployment", id));
    }

    /**
     * A deployment is triggered either by a signed-in person or by a build agent's key. The label
     * is always populated so history stays readable even after the account behind it is deleted.
     */
    private Actor currentActor() {
        UserPrincipal user = CurrentUser.principalOrNull();
        if (user != null) {
            return new Actor(user.getId(), user.getUsername(),
                    userRepository.findById(user.getId()).orElse(null));
        }

        ApiKeyPrincipal key = CurrentApiKey.principal().orElse(null);
        if (key != null) {
            return new Actor(null, key.auditLabel(), null);
        }
        return new Actor(null, "system", null);
    }

    private static AuditAction auditActionFor(DeploymentStatus status) {
        return switch (status) {
            case RUNNING -> AuditAction.DEPLOYMENT_STARTED;
            case SUCCESS -> AuditAction.DEPLOYMENT_SUCCEEDED;
            case FAILED -> AuditAction.DEPLOYMENT_FAILED;
            case CANCELLED -> AuditAction.DEPLOYMENT_CANCELLED;
            case PENDING -> AuditAction.DEPLOYMENT_CREATED;
        };
    }

    private static String pastTense(DeploymentStatus status) {
        return switch (status) {
            case RUNNING -> "Started";
            case SUCCESS -> "Deployed";
            case FAILED -> "Failed deploying";
            case CANCELLED -> "Cancelled";
            case PENDING -> "Queued";
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Actor(UUID id, String label, User user) {
    }
}
