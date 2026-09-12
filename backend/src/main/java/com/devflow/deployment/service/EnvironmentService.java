package com.devflow.deployment.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.deployment.dto.EnvironmentRequest;
import com.devflow.deployment.dto.EnvironmentResponse;
import com.devflow.deployment.entity.Environment;
import com.devflow.deployment.mapper.DeploymentMapper;
import com.devflow.deployment.repository.DeploymentRepository;
import com.devflow.deployment.repository.EnvironmentRepository;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ConflictException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.project.entity.Project;
import com.devflow.project.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final DeploymentRepository deploymentRepository;
    private final ProjectService projectService;
    private final DeploymentMapper deploymentMapper;
    private final AuditService auditService;

    public EnvironmentService(EnvironmentRepository environmentRepository,
                              DeploymentRepository deploymentRepository,
                              ProjectService projectService,
                              DeploymentMapper deploymentMapper,
                              AuditService auditService) {
        this.environmentRepository = environmentRepository;
        this.deploymentRepository = deploymentRepository;
        this.projectService = projectService;
        this.deploymentMapper = deploymentMapper;
        this.auditService = auditService;
    }

    public List<EnvironmentResponse> list(UUID projectId) {
        projectService.requireProject(projectId);
        return environmentRepository.findByProjectIdOrderByNameAsc(projectId).stream()
                .map(deploymentMapper::toResponse)
                .toList();
    }

    @Transactional
    public EnvironmentResponse create(UUID projectId, EnvironmentRequest request) {
        Project project = projectService.requireActiveProject(projectId);
        String name = request.name().trim();
        if (environmentRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ConflictException("An environment named %s already exists in this project".formatted(name));
        }

        Environment environment = new Environment(project, name, request.type());
        environment.setUrl(trimToNull(request.url()));
        environment.setRequiresApproval(Boolean.TRUE.equals(request.requiresApproval()));
        environmentRepository.save(environment);

        auditService.record(AuditAction.ENVIRONMENT_CREATED, "Environment", environment.getId(), projectId,
                "Added environment %s to %s".formatted(name, project.getProjectKey()),
                Map.of("type", request.type().name(),
                        "requiresApproval", environment.isRequiresApproval()));

        return deploymentMapper.toResponse(environment);
    }

    @Transactional
    public EnvironmentResponse update(UUID projectId, UUID environmentId, EnvironmentRequest request) {
        projectService.requireActiveProject(projectId);
        Environment environment = requireEnvironment(projectId, environmentId);

        String name = request.name().trim();
        if (!environment.getName().equalsIgnoreCase(name)
                && environmentRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ConflictException("An environment named %s already exists in this project".formatted(name));
        }

        environment.setName(name);
        environment.setType(request.type());
        environment.setUrl(trimToNull(request.url()));
        environment.setRequiresApproval(Boolean.TRUE.equals(request.requiresApproval()));

        return deploymentMapper.toResponse(environment);
    }

    /**
     * Refused while any deployment still points at it. The database would refuse too, but only as
     * an opaque integrity error; deployment history is the point of this application, so losing it
     * to a rename-by-delete is worth an explicit rule.
     */
    @Transactional
    public void delete(UUID projectId, UUID environmentId) {
        projectService.requireActiveProject(projectId);
        Environment environment = requireEnvironment(projectId, environmentId);

        if (deploymentRepository.existsByEnvironmentId(environmentId)) {
            throw new BusinessRuleException(
                    "Environment %s has deployment history and cannot be deleted".formatted(environment.getName()));
        }

        auditService.record(AuditAction.ENVIRONMENT_DELETED, "Environment", environment.getId(), projectId,
                "Removed environment %s".formatted(environment.getName()), Map.of());

        environmentRepository.delete(environment);
    }

    public Environment requireByName(UUID projectId, String name) {
        return environmentRepository.findByProjectIdAndNameIgnoreCase(projectId, name.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project has no environment named %s".formatted(name)));
    }

    private Environment requireEnvironment(UUID projectId, UUID environmentId) {
        return environmentRepository.findByIdAndProjectId(environmentId, projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Environment", environmentId));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
