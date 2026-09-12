package com.devflow.project.service;

import com.devflow.project.repository.ProjectMemberRepository;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.security.CurrentUser;
import com.devflow.security.UserPrincipal;
import com.devflow.user.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The single authority on "may this user touch this project", exposed to method security as
 * {@code @PreAuthorize("@projectAccess.canWrite(#projectId)")}.
 *
 * <p>Two tiers decide the answer. An account-wide ADMIN bypasses every project check. For everyone
 * else the project role governs, independently of the account-wide role: a global VIEWER who has
 * been added to a project as a DEVELOPER can write in that project and nowhere else.
 *
 * <p>An unknown project id is denied rather than reported as missing, so callers who are not
 * members cannot probe which project ids exist. Members and global admins still get a 404 from
 * the service layer, because their check passes first.
 */
@Service("projectAccess")
@Transactional(readOnly = true)
public class ProjectAccessService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;

    public ProjectAccessService(ProjectRepository projectRepository, ProjectMemberRepository memberRepository) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
    }

    public boolean canRead(UUID projectId) {
        return hasProjectRole(projectId, Role.VIEWER);
    }

    public boolean canWrite(UUID projectId) {
        return hasProjectRole(projectId, Role.DEVELOPER);
    }

    public boolean canAdmin(UUID projectId) {
        return hasProjectRole(projectId, Role.ADMIN);
    }

    private boolean hasProjectRole(UUID projectId, Role required) {
        UserPrincipal principal = CurrentUser.principalOrNull();
        if (principal == null || projectId == null) {
            return false;
        }
        if (principal.getRole() == Role.ADMIN) {
            return true;
        }
        // The owner is always kept in the member table as an ADMIN; checking ownership as well
        // means a permission decision never depends on that invariant holding.
        if (projectRepository.existsByIdAndOwnerId(projectId, principal.getId())) {
            return true;
        }
        return memberRepository.findRole(projectId, principal.getId())
                .filter(role -> role.isAtLeast(required))
                .isPresent();
    }
}
