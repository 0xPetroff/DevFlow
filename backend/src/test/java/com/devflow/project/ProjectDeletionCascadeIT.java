package com.devflow.project;

import com.devflow.deployment.entity.EnvironmentType;
import com.devflow.deployment.repository.DeploymentRepository;
import com.devflow.deployment.repository.EnvironmentRepository;
import com.devflow.issue.repository.CommentRepository;
import com.devflow.issue.repository.IssueRepository;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestDeployments;
import com.devflow.support.TestIssues;
import com.devflow.support.TestProjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Deleting a project cascades to both environments and deployments. PostgreSQL runs the
 * environments cascade first, and the old RESTRICT on deployments.environment_id was checked
 * immediately, so the delete failed while the deployment rows were still there. V2 relaxed that
 * one constraint to NO ACTION, which defers the check to the end of the statement.
 */
@IntegrationTest
class ProjectDeletionCascadeIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestIssues issues;

    @Autowired
    private TestDeployments deployments;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        token = accounts.registerAdmin("boss");
    }

    @Test
    void archivingThenDeletingAFullProjectRemovesEverythingInIt() throws Exception {
        UUID projectId = projects.create(token, "DEVF", "DevFlow");
        deployments.createEnvironment(token, projectId, "staging", EnvironmentType.STAGING, false);
        deployments.createEnvironment(token, projectId, "production", EnvironmentType.PRODUCTION, true);

        UUID finished = deployments.deploy(token, projectId, "staging", "1.0.0");
        deployments.transition(token, finished, "RUNNING");
        deployments.transition(token, finished, "SUCCESS");
        deployments.deploy(token, projectId, "staging", "1.0.1");

        UUID label = issues.createLabel(token, projectId, "backend", "#6366f1");
        UUID issueId = issues.createFrom(token, projectId, """
                {"title":"Something to clean up","labelIds":["%s"]}""".formatted(label));
        issues.comment(token, issueId, "A comment that should go too");

        projects.archive(token, projectId, "DevFlow");

        mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(projectId)).isEmpty();
        assertThat(deploymentRepository.count()).isZero();
        assertThat(environmentRepository.count()).isZero();
        assertThat(issueRepository.count()).isZero();
        assertThat(commentRepository.count()).isZero();
    }

    @Test
    void aProjectWithEnvironmentsButNoDeploymentsStillDeletes() throws Exception {
        UUID projectId = projects.create(token, "OPS", "Operations");
        deployments.createEnvironment(token, projectId, "staging", EnvironmentType.STAGING, false);
        projects.archive(token, projectId, "Operations");

        mockMvc.perform(delete("/api/projects/" + projectId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(environmentRepository.count()).isZero();
    }

    @Test
    void deletingOneProjectLeavesAnotherUntouched() throws Exception {
        UUID doomed = projects.create(token, "DEVF", "DevFlow");
        UUID survivor = projects.create(token, "OPS", "Operations");
        deployments.createEnvironment(token, doomed, "staging", EnvironmentType.STAGING, false);
        deployments.createEnvironment(token, survivor, "staging", EnvironmentType.STAGING, false);
        deployments.deploy(token, doomed, "staging", "1.0.0");
        deployments.deploy(token, survivor, "staging", "1.0.0");

        projects.archive(token, doomed, "DevFlow");
        mockMvc.perform(delete("/api/projects/" + doomed).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(projectRepository.findById(survivor)).isPresent();
        assertThat(deploymentRepository.count()).isEqualTo(1);
        assertThat(environmentRepository.count()).isEqualTo(1);
    }
}
