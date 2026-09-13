package com.devflow.support;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Drives issue and label setup through the real HTTP API. */
@TestComponent
public class TestIssues {

    private final MockMvc mockMvc;
    private final JsonMapper jsonMapper;

    public TestIssues(MockMvc mockMvc, JsonMapper jsonMapper) {
        this.mockMvc = mockMvc;
        this.jsonMapper = jsonMapper;
    }

    public UUID create(String token, UUID projectId, String title) throws Exception {
        return createFrom(token, projectId, """
                {"title":"%s"}""".formatted(title));
    }

    public UUID createFrom(String token, UUID projectId, String jsonBody) throws Exception {
        String body = mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    public UUID createLabel(String token, UUID projectId, String name, String color) throws Exception {
        String body = mockMvc.perform(post("/api/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","color":"%s"}""".formatted(name, color)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }

    public UUID comment(String token, UUID issueId, String text) throws Exception {
        String body = mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"%s"}""".formatted(text)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(jsonMapper.readTree(body).get("id").asString());
    }
}
