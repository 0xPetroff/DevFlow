package com.devflow.issue;

import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestIssues;
import com.devflow.support.TestProjects;
import com.devflow.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class LabelControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestIssues issues;

    private String ownerToken;
    private UUID projectId;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        ownerToken = accounts.registerAdmin("boss");
        projectId = projects.create(ownerToken, "DEVF", "DevFlow");
    }

    @Test
    void labelsAreCreatedListedAndEdited() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"backend","color":"#6366f1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("backend"));

        UUID labelId = issues.createLabel(ownerToken, projectId, "frontend", "#10b981");

        mockMvc.perform(get("/api/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("backend"));

        mockMvc.perform(put("/api/projects/" + projectId + "/labels/" + labelId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"ui","color":"#f59e0b"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ui"))
                .andExpect(jsonPath("$.color").value("#f59e0b"));
    }

    @Test
    void labelNamesAreUniqueWithinAProjectButNotAcrossProjects() throws Exception {
        issues.createLabel(ownerToken, projectId, "backend", "#6366f1");

        mockMvc.perform(post("/api/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"BACKEND","color":"#6366f1"}"""))
                .andExpect(status().isConflict());

        UUID otherProject = projects.create(ownerToken, "OPS", "Operations");
        mockMvc.perform(post("/api/projects/" + otherProject + "/labels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"backend","color":"#6366f1"}"""))
                .andExpect(status().isCreated());
    }

    @Test
    void malformedColoursAreRejected() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"bad","color":"red"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("color"));
    }

    @Test
    void deletingALabelRemovesItFromItsIssues() throws Exception {
        UUID labelId = issues.createLabel(ownerToken, projectId, "backend", "#6366f1");
        UUID issueId = issues.createFrom(ownerToken, projectId, """
                {"title":"Labelled","labelIds":["%s"]}""".formatted(labelId));

        mockMvc.perform(delete("/api/projects/" + projectId + "/labels/" + labelId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/issues/" + issueId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels").isEmpty());
    }

    @Test
    void writersManageLabelsButOnlyAdministratorsDeleteThem() throws Exception {
        var developer = accounts.register("dev", Role.DEVELOPER);
        var viewer = accounts.register("watcher", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, developer.id(), Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, viewer.id(), Role.VIEWER);
        UUID labelId = issues.createLabel(ownerToken, projectId, "backend", "#6366f1");

        mockMvc.perform(post("/api/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + developer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"created-by-a-developer","color":"#10b981"}"""))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/projects/" + projectId + "/labels")
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"created-by-a-viewer","color":"#10b981"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/" + projectId + "/labels/" + labelId)
                        .header("Authorization", "Bearer " + developer.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void labelsOfAnotherProjectAreNotReachableThroughThisOne() throws Exception {
        UUID otherProject = projects.create(ownerToken, "OPS", "Operations");
        UUID foreignLabel = issues.createLabel(ownerToken, otherProject, "ops", "#10b981");

        mockMvc.perform(put("/api/projects/" + projectId + "/labels/" + foreignLabel)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"stolen","color":"#6366f1"}"""))
                .andExpect(status().isNotFound());
    }
}
