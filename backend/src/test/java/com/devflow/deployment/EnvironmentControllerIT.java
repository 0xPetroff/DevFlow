package com.devflow.deployment;

import com.devflow.deployment.entity.EnvironmentType;
import com.devflow.deployment.repository.EnvironmentRepository;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestDeployments;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class EnvironmentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestDeployments deployments;

    private String ownerToken;
    private UUID projectId;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        ownerToken = accounts.registerAdmin("boss");
        projectId = projects.create(ownerToken, "DEVF", "DevFlow");
    }

    @Test
    void environmentsAreCreatedListedAndUpdated() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/environments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"staging","type":"STAGING","url":"https://staging.example.com"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("staging"))
                .andExpect(jsonPath("$.type").value("STAGING"))
                .andExpect(jsonPath("$.requiresApproval").value(false));

        UUID production = deployments.createEnvironment(ownerToken, projectId, "production",
                EnvironmentType.PRODUCTION, true);

        mockMvc.perform(get("/api/projects/" + projectId + "/environments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("production"))
                .andExpect(jsonPath("$[0].requiresApproval").value(true));

        mockMvc.perform(put("/api/projects/" + projectId + "/environments/" + production)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"prod","type":"PRODUCTION","requiresApproval":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("prod"))
                .andExpect(jsonPath("$.requiresApproval").value(false));
    }

    @Test
    void environmentNamesAreUniqueWithinTheProject() throws Exception {
        deployments.createEnvironment(ownerToken, projectId, "staging", EnvironmentType.STAGING, false);

        mockMvc.perform(post("/api/projects/" + projectId + "/environments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"STAGING","type":"STAGING"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void onlyProjectAdministratorsManageEnvironments() throws Exception {
        var developer = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, developer.id(), Role.DEVELOPER);
        UUID environmentId = deployments.createEnvironment(ownerToken, projectId, "staging",
                EnvironmentType.STAGING, false);

        mockMvc.perform(get("/api/projects/" + projectId + "/environments")
                        .header("Authorization", "Bearer " + developer.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/" + projectId + "/environments")
                        .header("Authorization", "Bearer " + developer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"sneaky","type":"DEVELOPMENT"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/" + projectId + "/environments/" + environmentId)
                        .header("Authorization", "Bearer " + developer.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anEnvironmentWithDeploymentHistoryCannotBeDeleted() throws Exception {
        UUID environmentId = deployments.createEnvironment(ownerToken, projectId, "staging",
                EnvironmentType.STAGING, false);
        deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        mockMvc.perform(delete("/api/projects/" + projectId + "/environments/" + environmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail")
                        .value("Environment staging has deployment history and cannot be deleted"));

        assertThat(environmentRepository.findById(environmentId)).isPresent();
    }

    @Test
    void anUnusedEnvironmentCanBeDeleted() throws Exception {
        UUID environmentId = deployments.createEnvironment(ownerToken, projectId, "scratch",
                EnvironmentType.DEVELOPMENT, false);

        mockMvc.perform(delete("/api/projects/" + projectId + "/environments/" + environmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertThat(environmentRepository.findById(environmentId)).isEmpty();
    }

    @Test
    void environmentsOfAnotherProjectAreNotReachableThroughThisOne() throws Exception {
        UUID otherProject = projects.create(ownerToken, "OPS", "Operations");
        UUID foreign = deployments.createEnvironment(ownerToken, otherProject, "staging",
                EnvironmentType.STAGING, false);

        mockMvc.perform(delete("/api/projects/" + projectId + "/environments/" + foreign)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void nonMembersCannotSeeEnvironments() throws Exception {
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        deployments.createEnvironment(ownerToken, projectId, "staging", EnvironmentType.STAGING, false);

        mockMvc.perform(get("/api/projects/" + projectId + "/environments")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }
}
