package com.devflow.project.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ConflictException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.project.dto.AddMemberRequest;
import com.devflow.project.dto.ProjectMemberResponse;
import com.devflow.project.dto.UpdateMemberRoleRequest;
import com.devflow.project.entity.Project;
import com.devflow.project.entity.ProjectMember;
import com.devflow.project.mapper.ProjectMemberMapper;
import com.devflow.project.repository.ProjectMemberRepository;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ProjectMemberMapper memberMapper;
    private final AuditService auditService;

    public ProjectMemberService(ProjectMemberRepository memberRepository,
                                UserRepository userRepository,
                                ProjectService projectService,
                                ProjectMemberMapper memberMapper,
                                AuditService auditService) {
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.memberMapper = memberMapper;
        this.auditService = auditService;
    }

    public List<ProjectMemberResponse> list(UUID projectId) {
        Project project = projectService.requireProject(projectId);
        return memberRepository.findByProjectIdOrderByCreatedAtAsc(projectId).stream()
                .map(member -> toResponse(project, member))
                .toList();
    }

    @Transactional
    public ProjectMemberResponse add(UUID projectId, AddMemberRequest request) {
        Project project = projectService.requireActiveProject(projectId);
        if (memberRepository.existsByProjectIdAndUserId(projectId, request.userId())) {
            throw new ConflictException("This user is already a member of the project");
        }

        User user = requireUser(request.userId());
        if (!user.isActive()) {
            throw new BusinessRuleException("A deactivated account cannot be added to a project");
        }

        ProjectMember member = memberRepository.save(new ProjectMember(project, user, request.role()));

        auditService.record(AuditAction.MEMBER_ADDED, "ProjectMember", member.getId(), projectId,
                "Added %s to %s as %s".formatted(user.getUsername(), project.getProjectKey(), request.role()),
                Map.of("userId", user.getId().toString(), "role", request.role().name()));

        return toResponse(project, member);
    }

    @Transactional
    public ProjectMemberResponse changeRole(UUID projectId, UUID userId, UpdateMemberRoleRequest request) {
        Project project = projectService.requireActiveProject(projectId);
        ProjectMember member = requireMember(projectId, userId);

        // The owner's implicit administrator rights cannot be revoked here; transfer ownership instead.
        if (isOwner(project, userId)) {
            throw new BusinessRuleException("The project owner's role cannot be changed");
        }

        Role previousRole = member.getRole();
        member.setRole(request.role());

        auditService.record(AuditAction.MEMBER_ROLE_CHANGED, "ProjectMember", member.getId(), projectId,
                "Changed %s in %s from %s to %s".formatted(member.getUser().getUsername(),
                        project.getProjectKey(), previousRole, request.role()),
                Map.of("userId", userId.toString(), "roleFrom", previousRole.name(),
                        "roleTo", request.role().name()));

        return toResponse(project, member);
    }

    @Transactional
    public void remove(UUID projectId, UUID userId) {
        Project project = projectService.requireActiveProject(projectId);
        ProjectMember member = requireMember(projectId, userId);

        if (isOwner(project, userId)) {
            throw new BusinessRuleException("The project owner cannot be removed from the project");
        }

        String username = member.getUser().getUsername();
        memberRepository.delete(member);

        auditService.record(AuditAction.MEMBER_REMOVED, "ProjectMember", member.getId(), projectId,
                "Removed %s from %s".formatted(username, project.getProjectKey()),
                Map.of("userId", userId.toString()));
    }

    private ProjectMemberResponse toResponse(Project project, ProjectMember member) {
        return memberMapper.toResponse(member, isOwner(project, member.getUser().getId()));
    }

    private boolean isOwner(Project project, UUID userId) {
        return project.getOwner().getId().equals(userId);
    }

    private ProjectMember requireMember(UUID projectId, UUID userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User %s is not a member of this project".formatted(userId)));
    }

    private User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
