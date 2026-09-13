package com.devflow.apikey;

import com.devflow.apikey.repository.ApiKeyRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class ApiKeyControllerIT {

    private static final String KEY_HEADER = "X-DevFlow-Api-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

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
    void issuingAKeyShowsItOnceAndStoresOnlyItsHash() throws Exception {
        String body = mockMvc.perform(post("/api/projects/" + projectId + "/api-keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"github-actions","expiresInDays":30}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").exists())
                .andExpect(jsonPath("$.apiKey.name").value("github-actions"))
                .andExpect(jsonPath("$.apiKey.scopes[0]").value("deployment:write"))
                .andExpect(jsonPath("$.apiKey.active").value(true))
                .andExpect(jsonPath("$.apiKey.expiresAt").exists())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("dfk_");

        mockMvc.perform(get("/api/projects/" + projectId + "/api-keys")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].key").doesNotExist())
                .andExpect(jsonPath("$[0].keyHash").doesNotExist())
                .andExpect(jsonPath("$[0].createdBy.username").value("boss"));

        var stored = apiKeyRepository.findAll().getFirst();
        assertThat(stored.getKeyHash()).hasSize(64).doesNotContain("dfk_");
    }

    @Test
    void aKeyDeploysToItsOwnProjectAndIsAttributedToTheAgent() throws Exception {
        String key = deployments.issueApiKey(ownerToken, projectId, "github-actions");

        mockMvc.perform(post("/api/deployments")
                        .header(KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "2.0.0")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.triggeredBy").doesNotExist())
                .andExpect(jsonPath("$.triggeredByLabel").value("api-key:github-actions"));

        assertThat(apiKeyRepository.findAll().getFirst().getLastUsedAt()).isNotNull();
    }

    @Test
    void aKeyDrivesTheWholeStateMachineForItsProject() throws Exception {
        String key = deployments.issueApiKey(ownerToken, projectId, "github-actions");
        UUID deploymentId = deployments.deployWithKey(key, projectId, "staging", "2.0.0");

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header(KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RUNNING"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header(KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUCCESS"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    /**
     * Issued keys used to be base64url, whose alphabet contains the underscore this format uses as
     * a separator, so roughly half of them could not be parsed back. One key proves nothing about
     * that; a batch does.
     */
    @Test
    void everyIssuedKeyHasTheSameShapeAndAuthenticates() throws Exception {
        for (int i = 0; i < 10; i++) {
            String key = deployments.issueApiKey(ownerToken, projectId, "agent-" + i);

            assertThat(key).matches("^dfk_[0-9a-f]{12}_[0-9a-f]{64}$");
            mockMvc.perform(post("/api/deployments")
                            .header(KEY_HEADER, key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deployments.deploymentBody(projectId, "staging", "1.0." + i)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void aKeyCannotTouchAnotherProject() throws Exception {
        UUID otherProject = projects.create(ownerToken, "OPS", "Operations");
        deployments.createEnvironment(ownerToken, otherProject, "staging", EnvironmentType.STAGING, false);
        String key = deployments.issueApiKey(ownerToken, projectId, "github-actions");

        mockMvc.perform(post("/api/deployments")
                        .header(KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(otherProject, "staging", "2.0.0")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aKeyIsUselessOutsideTheDeploymentEndpoints() throws Exception {
        String key = deployments.issueApiKey(ownerToken, projectId, "github-actions");

        mockMvc.perform(get("/api/users").header(KEY_HEADER, key))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/projects/" + projectId).header(KEY_HEADER, key))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/projects/" + projectId + "/issues").header(KEY_HEADER, key))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownAndMalformedKeysAreRejected() throws Exception {
        mockMvc.perform(post("/api/deployments")
                        .header(KEY_HEADER, "dfk_aaaaaaaa_totallymadeup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "2.0.0")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/deployments")
                        .header(KEY_HEADER, "not-even-a-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "2.0.0")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTamperedKeyWithAValidPrefixIsRejected() throws Exception {
        String key = deployments.issueApiKey(ownerToken, projectId, "github-actions");
        String prefix = key.split("_")[1];

        mockMvc.perform(post("/api/deployments")
                        .header(KEY_HEADER, "dfk_%s_wrongsecret".formatted(prefix))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "2.0.0")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokingAKeyStopsItWorkingButKeepsTheRecord() throws Exception {
        String key = deployments.issueApiKey(ownerToken, projectId, "github-actions");
        UUID keyId = apiKeyRepository.findAll().getFirst().getId();

        mockMvc.perform(delete("/api/projects/" + projectId + "/api-keys/" + keyId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/deployments")
                        .header(KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "2.0.0")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/projects/" + projectId + "/api-keys")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false))
                .andExpect(jsonPath("$[0].revokedAt").exists());
    }

    @Test
    void onlyProjectAdministratorsManageKeys() throws Exception {
        var developer = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, developer.id(), Role.DEVELOPER);

        mockMvc.perform(post("/api/projects/" + projectId + "/api-keys")
                        .header("Authorization", "Bearer " + developer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"sneaky"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/" + projectId + "/api-keys")
                        .header("Authorization", "Bearer " + developer.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void aBearerTokenWinsWhenBothCredentialsArePresent() throws Exception {
        String key = deployments.issueApiKey(ownerToken, projectId, "github-actions");

        mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header(KEY_HEADER, key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deployments.deploymentBody(projectId, "staging", "2.0.0")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.triggeredByLabel").value("boss"));
    }
}
