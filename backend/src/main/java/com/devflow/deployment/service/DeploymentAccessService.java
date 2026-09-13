package com.devflow.deployment.service;

import com.devflow.apikey.entity.ApiKey;
import com.devflow.deployment.repository.DeploymentRepository;
import com.devflow.project.service.ProjectAccessService;
import com.devflow.security.ApiKeyPrincipal;
import com.devflow.security.CurrentApiKey;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Deployment endpoints answer to two kinds of caller, so they use this instead of projectAccess
 * directly: {@code @PreAuthorize("@deploymentAccess.canWrite(#projectId)")}.
 *
 * <p>A person is judged exactly as everywhere else, by their project role. A build agent is judged
 * by its key: it must be scoped to this very project and hold deployment:write, which is why a
 * leaked key cannot be turned on any other project, and why it never confers administrator rights.
 */
@Service("deploymentAccess")
@Transactional(readOnly = true)
public class DeploymentAccessService {

    private final ProjectAccessService projectAccess;
    private final DeploymentRepository deploymentRepository;

    public DeploymentAccessService(ProjectAccessService projectAccess,
                                   DeploymentRepository deploymentRepository) {
        this.projectAccess = projectAccess;
        this.deploymentRepository = deploymentRepository;
    }

    public boolean canRead(UUID projectId) {
        return projectAccess.canRead(projectId) || keyFor(projectId).isPresent();
    }

    public boolean canWrite(UUID projectId) {
        return projectAccess.canWrite(projectId)
                || keyFor(projectId).filter(key -> key.hasScope(ApiKey.SCOPE_DEPLOYMENT_WRITE)).isPresent();
    }

    public boolean canReadDeployment(UUID deploymentId) {
        return projectOf(deploymentId).map(this::canRead).orElse(false);
    }

    public boolean canWriteDeployment(UUID deploymentId) {
        return projectOf(deploymentId).map(this::canWrite).orElse(false);
    }

    private Optional<ApiKeyPrincipal> keyFor(UUID projectId) {
        return projectId == null
                ? Optional.empty()
                : CurrentApiKey.principal().filter(key -> key.isForProject(projectId));
    }

    private Optional<UUID> projectOf(UUID deploymentId) {
        return deploymentId == null ? Optional.empty() : deploymentRepository.findProjectId(deploymentId);
    }
}
