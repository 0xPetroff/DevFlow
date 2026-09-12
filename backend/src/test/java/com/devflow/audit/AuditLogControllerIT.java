package com.devflow.audit;

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
class AuditLogControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestIssues issues;

    private String adminToken;
    private UUID projectId;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        adminToken = accounts.registerAdmin("boss");
        projectId = projects.create(adminToken, "DEVF", "DevFlow");
    }

    @Test
    void projectActivityIsListedNewestFirst() throws Exception {
        issues.create(adminToken, projectId, "Wire the deploy hook");

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("ISSUE_CREATED"))
                .andExpect(jsonPath("$.content[0].actorLabel").value("boss"))
                .andExpect(jsonPath("$.content[0].entityType").value("Issue"))
                .andExpect(jsonPath("$.content[1].action").value("PROJECT_CREATED"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void theRecordedIpAddressIsNeverReturned() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ipAddress").doesNotExist());
    }

    @Test
    void entriesAreFilteredByActionAndActor() throws Exception {
        var developer = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(adminToken, projectId, developer.id(), Role.DEVELOPER);
        issues.create(adminToken, projectId, "Owned by the admin");
        issues.create(developer.token(), projectId, "Owned by the developer");

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .param("action", "ISSUE_CREATED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .param("action", "ISSUE_CREATED")
                        .param("actorId", developer.id().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].actorLabel").value("dev"));
    }

    @Test
    void theTextSearchMatchesTheSummary() throws Exception {
        issues.create(adminToken, projectId, "Wire the deploy hook");

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .param("q", "deploy hook")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("ISSUE_CREATED"));
    }

    @Test
    void aProjectMemberSeesItsTrailAndAnOutsiderDoesNot() throws Exception {
        var viewer = accounts.register("viewer", Role.VIEWER);
        var outsider = accounts.register("outsider", Role.VIEWER);
        projects.addMember(adminToken, projectId, viewer.id(), Role.VIEWER);

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + viewer.token()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void oneProjectsTrailNeverLeaksIntoAnothers() throws Exception {
        UUID otherProjectId = projects.create(adminToken, "OTHR", "Other");
        issues.create(adminToken, otherProjectId, "Not visible from DEVF");

        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].action").value("PROJECT_CREATED"));
    }

    @Test
    void theAccountWideTrailIsAdministratorsOnly() throws Exception {
        var developer = accounts.register("dev", Role.DEVELOPER);

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("USER_CREATED"))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/audit-logs")
                        .header("Authorization", "Bearer " + developer.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownSortPropertyIsRejected() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectId + "/audit-logs")
                        .param("sort", "ipAddress,desc")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
}
