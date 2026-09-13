package com.devflow.deployment;

import com.devflow.deployment.entity.EnvironmentType;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Environment.requiresApproval is the manual gate in front of production: CI may queue the
 * release, but only a project administrator can let it run.
 */
@IntegrationTest
class DeploymentApprovalIT {

    private static final String KEY_HEADER = "X-DevFlow-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestDeployments deployments;

    private String ownerToken;
    private UUID projectId;
    private String apiKey;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        ownerToken = accounts.registerAdmin("boss");
        projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        deployments.createEnvironment(ownerToken, projectId, "staging", EnvironmentType.STAGING, false);
        deployments.createEnvironment(ownerToken, projectId, "production", EnvironmentType.PRODUCTION, true);
        apiKey = deployments.issueApiKey(ownerToken, projectId, "github-actions");
    }

    @Test
    void aPipelineQueuesAProductionReleaseButCannotStartIt() throws Exception {
        UUID deploymentId = deployments.deployWithKey(apiKey, projectId, "production", "3.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header(KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void onceApprovedThePipelineReportsTheOutcomeItself() throws Exception {
        UUID deploymentId = deployments.deployWithKey(apiKey, projectId, "production", "3.0.0");
        deployments.transition(ownerToken, deploymentId, "RUNNING");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header(KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUCCESS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void writeAccessAloneDoesNotApproveAProductionRelease() throws Exception {
        var developer = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, developer.id(), Role.DEVELOPER);
        UUID deploymentId = deployments.deploy(developer.token(), projectId, "production", "3.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + developer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Access denied"));
    }

    @Test
    void environmentsWithoutTheGateRunStraightThrough() throws Exception {
        UUID deploymentId = deployments.deployWithKey(apiKey, projectId, "staging", "3.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header(KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void cancellingAQueuedProductionReleaseNeedsNoApproval() throws Exception {
        UUID deploymentId = deployments.deployWithKey(apiKey, projectId, "production", "3.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header(KEY_HEADER, apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
