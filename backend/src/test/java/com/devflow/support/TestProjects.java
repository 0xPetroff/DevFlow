package com.devflow.support;

import com.devflow.user.entity.Role;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Drives project setup through the real HTTP API so tests never depend on repository shortcuts. */
@TestComponent
public class TestProjects {

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    public TestProjects(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
    }

    public UUID create(String token, String projectKey, String name) throws Exception {
        String body = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectKey":"%s","name":"%s"}""".formatted(projectKey, name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    public void addMember(String token, UUID projectId, UUID userId, Role role) throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/members")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","role":"%s"}""".formatted(userId, role)))
                .andExpect(status().isCreated());
    }

    public void archive(String token, UUID projectId, String name) throws Exception {
        mockMvc.perform(put("/api/projects/" + projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","status":"ARCHIVED"}""".formatted(name)))
                .andExpect(status().isOk());
    }
}
