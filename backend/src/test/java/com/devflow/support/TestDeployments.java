package com.devflow.support;

import com.devflow.deployment.entity.EnvironmentType;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Drives environment, deployment and API key setup through the real HTTP API. */
@TestComponent
public class TestDeployments {

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    public TestDeployments(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
    }

    public UUID createEnvironment(String token, UUID projectId, String name, EnvironmentType type,
                                  boolean requiresApproval) throws Exception {
        String body = mockMvc.perform(post("/api/projects/" + projectId + "/environments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","type":"%s","requiresApproval":%s}"""
                                .formatted(name, type, requiresApproval)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    public UUID deploy(String token, UUID projectId, String environmentName, String version) throws Exception {
        String body = mockMvc.perform(post("/api/deployments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deploymentBody(projectId, environmentName, version)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    public UUID deployWithKey(String apiKey, UUID projectId, String environmentName, String version)
            throws Exception {
        String body = mockMvc.perform(post("/api/deployments")
                        .header("X-DevFlow-Api-Key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deploymentBody(projectId, environmentName, version)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    public void transition(String token, UUID deploymentId, String status) throws Exception {
        mockMvc.perform(put("/api/deployments/" + deploymentId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"%s"}""".formatted(status)))
                .andExpect(status().isOk());
    }

    /** Returns the plaintext key, which the API shows exactly once. */
    public String issueApiKey(String token, UUID projectId, String name) throws Exception {
        String body = mockMvc.perform(post("/api/projects/" + projectId + "/api-keys")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}""".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(body).get("key").asString();
    }

    public String deploymentBody(UUID projectId, String environmentName, String version) {
        return """
                {"projectId":"%s","environmentName":"%s","releaseVersion":"%s",
                 "commitHash":"a1b2c3d4e5f6","branch":"main"}"""
                .formatted(projectId, environmentName, version);
    }
}
