package com.devflow.dashboard;

import com.devflow.deployment.entity.EnvironmentType;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestDeployments;
import com.devflow.support.TestIssues;
import com.devflow.support.TestProjects;
import com.devflow.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class DashboardControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestIssues issues;

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
    void theDashboardTotalsProjectsIssuesAndDeployments() throws Exception {
        issues.create(ownerToken, projectId, "First");
        issues.createFrom(ownerToken, projectId, """
                {"title":"Underway","status":"IN_PROGRESS"}""");
        issues.createFrom(ownerToken, projectId, """
                {"title":"Finished","status":"DONE"}""");

        UUID succeeded = deployments.deploy(ownerToken, projectId, "staging", "1.0.0");
        deployments.transition(ownerToken, succeeded, "RUNNING");
        deployments.transition(ownerToken, succeeded, "SUCCESS");

        UUID failed = deployments.deploy(ownerToken, projectId, "staging", "1.0.1");
        deployments.transition(ownerToken, failed, "RUNNING");
        deployments.transition(ownerToken, failed, "FAILED");

        UUID succeededAgain = deployments.deploy(ownerToken, projectId, "staging", "1.0.2");
        deployments.transition(ownerToken, succeededAgain, "RUNNING");
        deployments.transition(ownerToken, succeededAgain, "SUCCESS");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.total").value(1))
                .andExpect(jsonPath("$.projects.active").value(1))
                .andExpect(jsonPath("$.projects.archived").value(0))
                .andExpect(jsonPath("$.issues.total").value(3))
                .andExpect(jsonPath("$.issues.byStatus.TODO").value(1))
                .andExpect(jsonPath("$.issues.byStatus.IN_PROGRESS").value(1))
                .andExpect(jsonPath("$.issues.byStatus.DONE").value(1))
                .andExpect(jsonPath("$.issues.byStatus.IN_REVIEW").value(0))
                .andExpect(jsonPath("$.deployments.total").value(3))
                .andExpect(jsonPath("$.deployments.byStatus.SUCCESS").value(2))
                .andExpect(jsonPath("$.deployments.byStatus.FAILED").value(1))
                .andExpect(jsonPath("$.deployments.successRate").value(2.0 / 3))
                .andExpect(jsonPath("$.deployments.recent.length()").value(3))
                .andExpect(jsonPath("$.deployments.recent[0].releaseVersion").value("1.0.2"));
    }

    @Test
    void theSuccessRateIsAbsentUntilSomethingHasFinished() throws Exception {
        deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployments.total").value(1))
                .andExpect(jsonPath("$.deployments.successRate").doesNotExist());
    }

    @Test
    void figuresCoverOnlyTheProjectsTheCallerCanSee() throws Exception {
        issues.create(ownerToken, projectId, "Hidden from the outsider");
        deployments.deploy(ownerToken, projectId, "staging", "1.0.0");

        var outsider = accounts.register("outsider", Role.DEVELOPER);
        UUID theirProject = projects.create(outsider.token(), "OPS", "Operations");
        issues.create(outsider.token(), theirProject, "Theirs");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.total").value(1))
                .andExpect(jsonPath("$.issues.total").value(1))
                .andExpect(jsonPath("$.deployments.total").value(0));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.projects.total").value(2))
                .andExpect(jsonPath("$.issues.total").value(2));
    }

    @Test
    void assignedAndOverdueCountsAreAboutTheCaller() throws Exception {
        var member = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, member.id(), Role.DEVELOPER);

        issues.createFrom(ownerToken, projectId, """
                {"title":"Theirs and late","assigneeId":"%s","dueDate":"%s"}"""
                .formatted(member.id(), LocalDate.now().minusDays(3)));
        issues.createFrom(ownerToken, projectId, """
                {"title":"Theirs and done","assigneeId":"%s","status":"DONE","dueDate":"%s"}"""
                .formatted(member.id(), LocalDate.now().minusDays(3)));
        issues.create(ownerToken, projectId, "Nobody's");

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issues.assignedToMe").value(2))
                .andExpect(jsonPath("$.issues.overdue").value(1));

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.issues.assignedToMe").value(0));
    }

    @Test
    void aUserWithNoProjectsGetsZeroesRatherThanAnError() throws Exception {
        var newcomer = accounts.register("newcomer", Role.DEVELOPER);

        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + newcomer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects.total").value(0))
                .andExpect(jsonPath("$.issues.total").value(0))
                .andExpect(jsonPath("$.issues.byStatus.TODO").value(0))
                .andExpect(jsonPath("$.deployments.total").value(0))
                .andExpect(jsonPath("$.deployments.recent").isEmpty());
    }

    @Test
    void theDeploymentWindowIsConfigurableAndClamped() throws Exception {
        mockMvc.perform(get("/api/dashboard?windowDays=7").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployments.windowDays").value(7));

        mockMvc.perform(get("/api/dashboard?windowDays=9999").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deployments.windowDays").value(365));
    }
}
