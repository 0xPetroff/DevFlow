package com.devflow.project;

import com.devflow.project.entity.ProjectStatus;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestProjects;
import com.devflow.user.entity.Role;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ProjectControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @BeforeEach
    void reset() {
        accounts.deleteEverything();
    }

    @Test
    void creatorBecomesOwnerAndAdministratorOfTheProject() throws Exception {
        accounts.registerAdmin("boss");
        var developer = accounts.register("dev", Role.DEVELOPER);

        String location = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + developer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"DEVF","name":"DevFlow","description":"Tracking"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.projectKey").value("DEVF"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.owner.username").value("dev"))
                .andExpect(jsonPath("$.memberCount").value(1))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location + "/members").header("Authorization", "Bearer " + developer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].user.username").value("dev"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].owner").value(true));
    }

    @Test
    void accountWideViewersCannotCreateProjects() throws Exception {
        accounts.registerAdmin("boss");
        var viewer = accounts.register("readonly", Role.VIEWER);

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"NOPE","name":"Not allowed"}"""))
                .andExpect(status().isForbidden());

        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void projectKeysAreNormalisedAndMustBeUnique() throws Exception {
        String token = accounts.registerAdmin("boss");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"devf","name":"DevFlow"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectKey").value("DEVF"));

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"DEVF","name":"Duplicate"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Project key DEVF is already in use"));
    }

    @Test
    void malformedProjectKeysAreRejected() throws Exception {
        String token = accounts.registerAdmin("boss");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"1BAD","name":"Starts with a digit"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("projectKey"));

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"WAY-TOO-LONG","name":"Punctuation"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonMembersAreDeniedAndCannotTellWhetherTheProjectExists() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        UUID projectId = projects.create(adminToken, "DEVF", "DevFlow");

        mockMvc.perform(get("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountWideAdminsReachProjectsTheyAreNotMembersOf() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        var developer = accounts.register("dev", Role.DEVELOPER);
        UUID projectId = projects.create(developer.token(), "DEVF", "DevFlow");

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.username").value("dev"));

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed by the administrator","status":"ACTIVE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed by the administrator"));
    }

    @Test
    void projectDevelopersCanReadButNotAdministerTheProject() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var member = accounts.register("dev", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        projects.addMember(ownerToken, projectId, member.id(), Role.DEVELOPER);

        mockMvc.perform(get("/api/projects/" + projectId).header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hijacked","status":"ACTIVE"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void projectAdminsWhoDoNotOwnTheProjectCanStillAdministerIt() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var lead = accounts.register("lead", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        projects.addMember(ownerToken, projectId, lead.id(), Role.ADMIN);

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + lead.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Renamed by the lead","status":"ACTIVE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed by the lead"));
    }

    @Test
    void listingReturnsOnlyProjectsTheCallerBelongsTo() throws Exception {
        String adminToken = accounts.registerAdmin("boss");
        var developer = accounts.register("dev", Role.DEVELOPER);

        UUID shared = projects.create(adminToken, "SHARED", "Shared work");
        projects.create(adminToken, "SECRET", "Hidden work");
        projects.addMember(adminToken, shared, developer.id(), Role.VIEWER);
        projects.create(developer.token(), "OWNED", "Their own project");

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + developer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/projects").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void listingFiltersByTextAndStatus() throws Exception {
        String token = accounts.registerAdmin("boss");
        projects.create(token, "DEVF", "DevFlow");
        UUID legacy = projects.create(token, "OLD", "Legacy platform");
        projects.archive(token, legacy, "Legacy platform");

        mockMvc.perform(get("/api/projects?status=ACTIVE").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].projectKey").value("DEVF"));

        mockMvc.perform(get("/api/projects?q=legacy").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].projectKey").value("OLD"));

        mockMvc.perform(get("/api/projects?q=dev").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void aProjectMustBeArchivedBeforeItCanBeDeleted() throws Exception {
        String token = accounts.registerAdmin("boss");
        UUID projectId = projects.create(token, "DEVF", "DevFlow");

        mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Archive the project before deleting it"));

        projects.archive(token, projectId, "DevFlow");

        mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(projectId)).isEmpty();
    }

    @Test
    void projectsCanBeFetchedByKeyAndUnknownKeysAreDenied() throws Exception {
        String token = accounts.registerAdmin("boss");
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        projects.create(token, "DEVF", "DevFlow");

        mockMvc.perform(get("/api/projects/key/devf").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectKey").value("DEVF"));

        mockMvc.perform(get("/api/projects/key/DEVF").header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/key/NOSUCH").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void transferringOwnershipMakesTheNewOwnerAnAdministrator() throws Exception {
        String ownerToken = accounts.registerAdmin("boss");
        var successor = accounts.register("successor", Role.DEVELOPER);
        UUID projectId = projects.create(ownerToken, "DEVF", "DevFlow");

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"DevFlow","status":"ACTIVE","ownerId":"%s"}""".formatted(successor.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner.username").value("successor"))
                .andExpect(jsonPath("$.memberCount").value(2));

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + successor.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Now mine","status":"ACTIVE"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void archivingAndRestoringAProjectIsRecordedOnTheProject() throws Exception {
        String token = accounts.registerAdmin("boss");
        UUID projectId = projects.create(token, "DEVF", "DevFlow");

        projects.archive(token, projectId, "DevFlow");
        assertThat(projectRepository.findById(projectId).orElseThrow().getStatus())
                .isEqualTo(ProjectStatus.ARCHIVED);

        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"DevFlow","status":"ACTIVE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void anonymousCallersAreRejected() throws Exception {
        String token = accounts.registerAdmin("boss");
        UUID projectId = projects.create(token, "DEVF", "DevFlow");

        mockMvc.perform(get("/api/projects/" + projectId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());
    }
}
