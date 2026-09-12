package com.devflow.issue;

import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestIssues;
import com.devflow.support.TestProjects;
import com.devflow.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class IssueListingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestIssues issues;

    private String token;
    private UUID projectId;
    private UUID assigneeId;
    private UUID labelId;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        token = accounts.registerAdmin("boss");
        var developer = accounts.register("dev", Role.DEVELOPER);
        assigneeId = developer.id();
        projectId = projects.create(token, "DEVF", "DevFlow");
        projects.addMember(token, projectId, assigneeId, Role.DEVELOPER);
        labelId = issues.createLabel(token, projectId, "backend", "#6366f1");

        issues.createFrom(token, projectId, """
                {"title":"Fix the login bug","type":"BUG","priority":"CRITICAL",
                 "assigneeId":"%s","labelIds":["%s"],"dueDate":"2026-01-10"}"""
                .formatted(assigneeId, labelId));
        issues.createFrom(token, projectId, """
                {"title":"Add dark mode","type":"FEATURE","priority":"LOW","status":"IN_PROGRESS"}""");
        issues.createFrom(token, projectId, """
                {"title":"Tidy the build","type":"TASK","priority":"MEDIUM","dueDate":"2026-06-01"}""");
    }

    @Test
    void listingReturnsEverythingInTheProjectNewestFirst() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Tidy the build"));
    }

    @Test
    void listingFiltersByStatusPriorityAndType() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/issues?status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Add dark mode"));

        mockMvc.perform(get("/api/projects/" + projectId + "/issues?priority=CRITICAL&priority=LOW")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/projects/" + projectId + "/issues?type=BUG")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listingFiltersByAssigneeAndByBeingUnassigned() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/issues?assigneeId=" + assigneeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix the login bug"));

        mockMvc.perform(get("/api/projects/" + projectId + "/issues?unassigned=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void listingFiltersByLabelDueDateAndFreeText() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/issues?labelId=" + labelId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/projects/" + projectId + "/issues?dueBefore=2026-02-01")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Fix the login bug"));

        mockMvc.perform(get("/api/projects/" + projectId + "/issues?q=dark")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filtersCombineAsAnAnd() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/issues?type=BUG&status=IN_PROGRESS")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listingSortsAndPaginates() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/issues?sort=issueNumber,asc&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].issueNumber").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void sortingByAnUnknownFieldIsARequestErrorRatherThanACrash() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/issues?sort=passwordHash")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));

        mockMvc.perform(get("/api/users?sort=passwordHash").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/projects?sort=nonsense").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
