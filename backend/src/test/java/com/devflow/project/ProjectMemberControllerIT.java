package com.devflow.project;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.repository.AuditLogRepository;
import com.devflow.project.repository.ProjectMemberRepository;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestProjects;
import com.devflow.user.entity.Role;
import com.devflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProjectMemberControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @BeforeEach
    void reset() {
        accounts.deleteEverything();
    }

    @Test
    void aProjectAdminGrantsAndRevokesAccess() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var teammate = accounts.register("dev", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"DEVELOPER"}""".formatted(teammate.id())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").value("dev"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"))
                .andExpect(jsonPath("$.owner").value(false));

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + teammate.token()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/projects/" + projectId + "/members/" + teammate.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + teammate.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void addingTheSameUserTwiceConflicts() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var teammate = accounts.register("dev", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        projects.addMember(ownerToken, projectId, teammate.id(), Role.DEVELOPER);

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"VIEWER"}""".formatted(teammate.id())))
                .andExpect(status().isConflict());

        assertThat(memberRepository.countByProjectId(projectId)).isEqualTo(2);
    }

    @Test
    void addingAnUnknownUserIsNotFound() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"DEVELOPER"}""".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivatedAccountsCannotBeAdded() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var teammate = accounts.register("dev", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");

        var user = userRepository.findById(teammate.id()).orElseThrow();
        user.setActive(false);
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"DEVELOPER"}""".formatted(teammate.id())))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void projectDevelopersCannotManageMembership() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var member = accounts.register("dev", Role.DEVELOPER);
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        projects.addMember(ownerToken, projectId, member.id(), Role.DEVELOPER);

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"ADMIN"}""".formatted(outsider.id())))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/projects/" + projectId + "/members/" + member.id())
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isForbidden());

        assertThat(memberRepository.findByProjectIdAndUserId(projectId, member.id()).orElseThrow().getRole())
                .isEqualTo(Role.DEVELOPER);
    }

    @Test
    void changingAMembersRoleIsAppliedAndAudited() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var member = accounts.register("dev", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        projects.addMember(ownerToken, projectId, member.id(), Role.VIEWER);

        mockMvc.perform(put("/api/projects/" + projectId + "/members/" + member.id())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertThat(memberRepository.findByProjectIdAndUserId(projectId, member.id()).orElseThrow().getRole())
                .isEqualTo(Role.ADMIN);
        assertThat(auditLogRepository.findAll())
                .anyMatch(entry -> entry.getAction() == AuditAction.MEMBER_ROLE_CHANGED
                        && projectId.equals(entry.getProjectId()));
    }

    @Test
    void theOwnerCannotBeDemotedOrRemoved() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var owner = userRepository.findByUsername("boss").orElseThrow();
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");

        mockMvc.perform(put("/api/projects/" + projectId + "/members/" + owner.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"VIEWER"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("The project owner's role cannot be changed"));

        mockMvc.perform(delete("/api/projects/" + projectId + "/members/" + owner.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableContent());

        assertThat(memberRepository.countByProjectId(projectId)).isEqualTo(1);
    }

    @Test
    void membershipCannotBeChangedOnAnArchivedProject() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var teammate = accounts.register("dev", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        projects.archive(ownerToken, projectId, "DevFlow");

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"DEVELOPER"}""".formatted(teammate.id())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Project DEVF is archived and cannot be modified"));
    }

    @Test
    void accountWideAdminsManageMembershipOfProjectsTheyAreNotIn() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        var developer = accounts.register("dev", Role.DEVELOPER);
        var teammate = accounts.register("teammate", Role.DEVELOPER);
        UUID projectId = projects.create(developer.token(), "DEVF", "DevFlow");

        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"VIEWER"}""".formatted(teammate.id())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void removingAUserWhoIsNotAMemberIsNotFound() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");

        mockMvc.perform(delete("/api/projects/" + projectId + "/members/" + outsider.id())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void membersOfOtherProjectsCannotListTheseMembers() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        projects.create(outsider.token(), "OTHER", "Something else");

        mockMvc.perform(get("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }
}
