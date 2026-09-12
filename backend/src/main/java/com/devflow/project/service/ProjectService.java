package com.devflow.project.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.common.PageResponse;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ConflictException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.project.dto.CreateProjectRequest;
import com.devflow.project.dto.ProjectResponse;
import com.devflow.project.dto.UpdateProjectRequest;
import com.devflow.project.entity.Project;
import com.devflow.project.entity.ProjectMember;
import com.devflow.project.entity.ProjectStatus;
import com.devflow.project.mapper.ProjectMapper;
import com.devflow.project.repository.ProjectMemberRepository;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.project.repository.ProjectSpecifications;
import com.devflow.security.UserPrincipal;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService projectAccess;
    private final AuditService auditService;

    public ProjectService(ProjectRepository projectRepository,
                          ProjectMemberRepository memberRepository,
                          UserRepository userRepository,
                          ProjectMapper projectMapper,
                          ProjectAccessService projectAccess,
                          AuditService auditService) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.projectMapper = projectMapper;
        this.projectAccess = projectAccess;
        this.auditService = auditService;
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request, UUID ownerId) {
        String key = Project.normaliseKey(request.projectKey());
        if (projectRepository.existsByProjectKey(key)) {
            throw new ConflictException("Project key %s is already in use".formatted(key));
        }

        User owner = requireUser(ownerId);
        Project project = new Project(key, request.name().trim(), owner);
        project.setDescription(trimToNull(request.description()));
        project.setRepositoryUrl(trimToNull(request.repositoryUrl()));
        projectRepository.save(project);

        // The owner is also a member so that member listings and permission checks see one consistent set.
        memberRepository.save(new ProjectMember(project, owner, Role.ADMIN));

        auditService.record(AuditAction.PROJECT_CREATED, "Project", project.getId(), project.getId(),
                "Created project %s".formatted(key), Map.of("projectKey", key, "name", project.getName()));

        return projectMapper.toResponse(project, 1);
    }

    public ProjectResponse getById(UUID id) {
        Project project = requireProject(id);
        return projectMapper.toResponse(project, memberRepository.countByProjectId(id));
    }

    /**
     * Unknown and inaccessible keys both fail as denied. Resolving a missing key to a 404 would
     * turn this endpoint into an oracle for which project keys exist.
     */
    public ProjectResponse getByKey(String projectKey) {
        Project project = projectRepository.findByProjectKey(Project.normaliseKey(projectKey))
                .filter(candidate -> projectAccess.canRead(candidate.getId()))
                .orElseThrow(() -> new AccessDeniedException("No readable project with this key"));
        return projectMapper.toResponse(project, memberRepository.countByProjectId(project.getId()));
    }

    public PageResponse<ProjectResponse> search(String query, ProjectStatus status,
                                                UserPrincipal principal, Pageable pageable) {
        UUID scopeTo = principal.getRole() == Role.ADMIN ? null : principal.getId();
        Page<Project> page = projectRepository.findAll(
                ProjectSpecifications.matching(query, status, scopeTo), pageable);

        Map<UUID, Long> memberCounts = countMembers(page.getContent());
        return PageResponse.from(page.map(project ->
                projectMapper.toResponse(project, memberCounts.getOrDefault(project.getId(), 0L))));
    }

    @Transactional
    public ProjectResponse update(UUID id, UpdateProjectRequest request) {
        Project project = requireProject(id);
        ProjectStatus previousStatus = project.getStatus();

        project.setName(request.name().trim());
        project.setDescription(trimToNull(request.description()));
        project.setRepositoryUrl(trimToNull(request.repositoryUrl()));
        project.setStatus(request.status());

        if (request.ownerId() != null && !request.ownerId().equals(project.getOwner().getId())) {
            transferOwnership(project, request.ownerId());
        }

        Map<String, Object> metadata = new HashMap<>(Map.of("name", project.getName()));
        if (previousStatus != project.getStatus()) {
            metadata.put("statusFrom", previousStatus.name());
            metadata.put("statusTo", project.getStatus().name());
        }

        auditService.record(AuditAction.PROJECT_UPDATED, "Project", project.getId(), project.getId(),
                "Updated project %s".formatted(project.getProjectKey()), metadata);

        return projectMapper.toResponse(project, memberRepository.countByProjectId(project.getId()));
    }

    /**
     * Deleting cascades to every issue, comment, label, environment and deployment in the project,
     * so it is gated on the project having been archived first rather than on confirmation in the UI.
     */
    @Transactional
    public void delete(UUID id) {
        Project project = requireProject(id);
        if (!project.isArchived()) {
            throw new BusinessRuleException("Archive the project before deleting it");
        }

        // Recorded without a project id on purpose: audit_logs.project_id cascades on project
        // delete, so an entry tagged with this project would be erased by the very delete it records.
        auditService.record(AuditAction.PROJECT_DELETED, "Project", project.getId(), null,
                "Deleted project %s".formatted(project.getProjectKey()),
                Map.of("projectKey", project.getProjectKey(), "name", project.getName()));

        projectRepository.delete(project);
    }

    public Project requireProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", id));
    }

    /** Guards operations that must not mutate a frozen project; membership changes are one of them. */
    public Project requireActiveProject(UUID id) {
        Project project = requireProject(id);
        if (project.isArchived()) {
            throw new BusinessRuleException("Project %s is archived and cannot be modified"
                    .formatted(project.getProjectKey()));
        }
        return project;
    }

    private void transferOwnership(Project project, UUID newOwnerId) {
        User newOwner = requireUser(newOwnerId);
        if (!newOwner.isActive()) {
            throw new BusinessRuleException("A deactivated account cannot own a project");
        }

        project.setOwner(newOwner);
        // An owner who is not an admin member would be locked out of their own project.
        memberRepository.findByProjectIdAndUserId(project.getId(), newOwnerId)
                .ifPresentOrElse(member -> member.setRole(Role.ADMIN),
                        () -> memberRepository.save(new ProjectMember(project, newOwner, Role.ADMIN)));
    }

    private Map<UUID, Long> countMembers(List<Project> projects) {
        if (projects.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = projects.stream().map(Project::getId).toList();
        return memberRepository.countByProjectIds(ids).stream()
                .collect(Collectors.toMap(ProjectMemberRepository.MemberCount::getProjectId,
                        ProjectMemberRepository.MemberCount::getTotal));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
