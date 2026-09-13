package com.devflow.deployment;

import com.devflow.deployment.entity.DeploymentStatus;
import com.devflow.deployment.entity.EnvironmentType;
import com.devflow.deployment.repository.DeploymentRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class DeploymentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeploymentRepository deploymentRepository;

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
        deployments.createEnvironment(ownerToken, projectId, "staging", EnvironmentType.STAGING, false);
    }

    @Test
    void aDeploymentIsRecordedAsPendingAndAttributedToItsTrigger() throws Exception {
        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","environmentName":"staging","releaseVersion":"1.4.0",
                                 "commitHash":"A1B2C3D4E5F6","commitMessage":"Ship the board",
                                 "branch":"main","pipelineUrl":"https://ci.example.com/7"}"""
                                .formatted(projectId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.releaseVersion").value("1.4.0"))
                .andExpect(jsonPath("$.commitHash").value("a1b2c3d4e5f6"))
                .andExpect(jsonPath("$.environment.name").value("staging"))
                .andExpect(jsonPath("$.project.projectKey").value("DEVF"))
                .andExpect(jsonPath("$.triggeredBy.username").value("boss"))
                .andExpect(jsonPath("$.triggeredByLabel").value("boss"))
                .andExpect(jsonPath("$.startedAt").doesNotExist())
                .andExpect(jsonPath("$.durationSeconds").doesNotExist());
    }

    @Test
    void aRunThatSucceedsRecordsItsTimingsAndDuration() throws Exception {
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        deployments.transition(ownerToken, deploymentId, "RUNNING");
        mockMvc.perform(get("/api/deployments/" + deploymentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.finishedAt").doesNotExist());

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUCCESS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.finishedAt").exists())
                .andExpect(jsonPath("$.durationSeconds").value(0));
    }

    @Test
    void aFailedRunKeepsItsReason() throws Exception {
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");
        deployments.transition(ownerToken, deploymentId, "RUNNING");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"FAILED","failureReason":"Smoke tests did not pass"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("Smoke tests did not pass"));
    }

    @Test
    void aCancelledRunKeepsItsReason() throws Exception {
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED","failureReason":"No approval within 1800s"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.failureReason").value("No approval within 1800s"));
    }

    @Test
    void illegalTransitionsAreRefused() throws Exception {
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUCCESS"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("A PENDING deployment cannot move to SUCCESS"));

        deployments.transition(ownerToken, deploymentId, "RUNNING");
        deployments.transition(ownerToken, deploymentId, "SUCCESS");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("This deployment already finished as SUCCESS"));
    }

    @Test
    void cancellingBeforeTheRunStartsLeavesNoTimings() throws Exception {
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"CANCELLED"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.startedAt").doesNotExist())
                .andExpect(jsonPath("$.finishedAt").doesNotExist())
                .andExpect(jsonPath("$.durationSeconds").doesNotExist());
    }

    @Test
    void anUnknownEnvironmentNameIsNotFound() throws Exception {
        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "nowhere", "1.0.0")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project has no environment named nowhere"));
    }

    @Test
    void malformedCommitHashesAreRejected() throws Exception {
        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","environmentName":"staging","releaseVersion":"1.0.0",
                                 "commitHash":"nothex","branch":"main"}""".formatted(projectId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("commitHash"));
    }

    @Test
    void projectViewersCanReadDeploymentsButNotRecordThem() throws Exception {
        var viewer = accounts.register("watcher", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, viewer.id(), Role.VIEWER);
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        mockMvc.perform(get("/api/deployments/" + deploymentId)
                        .header("Authorization", "Bearer " + viewer.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "1.0.1")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMembersCannotSeeOrRecordDeployments() throws Exception {
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        mockMvc.perform(get("/api/deployments/" + deploymentId)
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + outsider.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "9.9.9")))
                .andExpect(status().isForbidden());

        assertThat(deploymentRepository.count()).isEqualTo(1);
    }

    @Test
    void listingFiltersByStatusEnvironmentAndBranch() throws Exception {
        UUID devEnvironmentId = deployments.createEnvironment(ownerToken, projectId, "dev",
                EnvironmentType.DEVELOPMENT, false);
        UUID first = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");
        deployments.deploy(ownerToken, projectId, "dev", "1.0.1");
        deployments.transition(ownerToken, first, "RUNNING");

        mockMvc.perform(get("/api/projects/" + projectId + "/deployments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/projects/" + projectId + "/deployments?status=RUNNING")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].releaseVersion").value("1.0.0"));

        mockMvc.perform(get("/api/projects/" + projectId + "/deployments?environmentId=" + devEnvironmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].environment.name").value("dev"));

        mockMvc.perform(get("/api/projects/" + projectId + "/deployments?branch=nosuchbranch")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.totalElements").value(0));

        mockMvc.perform(get("/api/projects/" + projectId + "/deployments?q=1.0.1")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deploymentsAreBlockedOnAnArchivedProject() throws Exception {
        UUID deploymentId = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");
        projects.archive(ownerToken, projectId, "DevFlow");

        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "1.0.1")))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isUnprocessableContent());

        assertThat(deploymentRepository.findById(deploymentId).orElseThrow().getStatus())
                .isEqualTo(DeploymentStatus.PENDING);
    }
}
