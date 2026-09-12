package com.devflow.project;

import com.devflow.project.entity.Project;
import com.devflow.project.entity.ProjectMember;
import com.devflow.project.repository.ProjectMemberRepository;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.project.service.ProjectAccessService;
import com.devflow.security.UserPrincipal;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The permission matrix itself, exercised directly rather than through whichever endpoints
 * happen to use it today. canWrite has no caller until issues arrive in the next stage.
 */
@IntegrationTest
class ProjectAccessServiceIT {

    @Autowired
    private ProjectAccessService projectAccess;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestAccounts accounts;

    private User owner;
    private Project project;

    @BeforeEach
    void setUp() {
        accounts.deleteEverything();
        owner = saveUser("owner", Role.DEVELOPER);
        project = projectRepository.saveAndFlush(new Project("DEVF", "DevFlow", owner));
        memberRepository.saveAndFlush(new ProjectMember(project, owner, Role.ADMIN));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void accountWideAdminsBypassMembershipEntirely() {
        authenticateAs(saveUser("boss", Role.ADMIN));

        assertAccess(true, true, true);
    }

    @Test
    void theOwnerHasFullControl() {
        authenticateAs(owner);

        assertAccess(true, true, true);
    }

    @Test
    void projectAdministratorsHaveFullControl() {
        authenticateAs(saveMember("lead", Role.DEVELOPER, Role.ADMIN));

        assertAccess(true, true, true);
    }

    @Test
    void projectDevelopersCanWriteButNotAdminister() {
        authenticateAs(saveMember("dev", Role.DEVELOPER, Role.DEVELOPER));

        assertAccess(true, true, false);
    }

    @Test
    void projectViewersCanOnlyRead() {
        authenticateAs(saveMember("watcher", Role.DEVELOPER, Role.VIEWER));

        assertAccess(true, false, false);
    }

    @Test
    void theProjectRoleGovernsRegardlessOfTheAccountWideRole() {
        authenticateAs(saveMember("contractor", Role.VIEWER, Role.DEVELOPER));

        assertAccess(true, true, false);
    }

    @Test
    void nonMembersHaveNoAccess() {
        authenticateAs(saveUser("outsider", Role.DEVELOPER));

        assertAccess(false, false, false);
    }

    @Test
    void unknownProjectsAreDeniedRatherThanReportedMissing() {
        authenticateAs(saveUser("outsider", Role.DEVELOPER));

        assertThat(projectAccess.canRead(UUID.randomUUID())).isFalse();
        assertThat(projectAccess.canRead(null)).isFalse();
    }

    @Test
    void unauthenticatedCallersHaveNoAccess() {
        SecurityContextHolder.clearContext();

        assertAccess(false, false, false);
    }

    private void assertAccess(boolean read, boolean write, boolean admin) {
        assertThat(projectAccess.canRead(project.getId())).isEqualTo(read);
        assertThat(projectAccess.canWrite(project.getId())).isEqualTo(write);
        assertThat(projectAccess.canAdmin(project.getId())).isEqualTo(admin);
    }

    private User saveMember(String username, Role accountRole, Role projectRole) {
        User user = saveUser(username, accountRole);
        memberRepository.saveAndFlush(new ProjectMember(project, user, projectRole));
        return user;
    }

    private User saveUser(String username, Role role) {
        return userRepository.saveAndFlush(
                new User(username + "@example.com", username, "not-a-real-hash", username, role));
    }

    private void authenticateAs(User user) {
        UserPrincipal principal = UserPrincipal.from(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
