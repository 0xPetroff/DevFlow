package com.devflow.issue;

import com.devflow.issue.repository.IssueRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class IssueControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IssueRepository issueRepository;

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
    void creatingAnIssueNumbersItAndDefaultsTheRest() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Wire up the board"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("DEVF-1"))
                .andExpect(jsonPath("$.issueNumber").value(1))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.type").value("TASK"))
                .andExpect(jsonPath("$.boardPosition").value(1000.0))
                .andExpect(jsonPath("$.creator.username").value("boss"))
                .andExpect(jsonPath("$.project.projectKey").value("DEVF"));
    }

    @Test
    void creatingAnIssueAcceptsEveryOptionalField() throws Exception {
        var member = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, member.id(), Role.DEVELOPER);
        UUID label = issues.createLabel(ownerToken, projectId, "backend", "#6366f1");

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Ship it","description":"With everything set",
                                 "status":"IN_PROGRESS","priority":"HIGH","type":"BUG",
                                 "assigneeId":"%s","dueDate":"2026-12-24","estimatePoints":8,
                                 "labelIds":["%s"]}""".formatted(member.id(), label)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.type").value("BUG"))
                .andExpect(jsonPath("$.assignee.username").value("dev"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-24"))
                .andExpect(jsonPath("$.estimatePoints").value(8))
                .andExpect(jsonPath("$.labels[0].name").value("backend"));
    }

    @Test
    void anAssigneeMustBeAMemberOfTheProject() throws Exception {
        var outsider = accounts.register("outsider", Role.DEVELOPER);

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Assigned to a stranger","assigneeId":"%s"}"""
                                .formatted(outsider.id())))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("The assignee must be a member of the project"));
    }

    @Test
    void labelsFromAnotherProjectAreRejected() throws Exception {
        UUID otherProject = projects.create(ownerToken, "OPS", "Operations");
        UUID foreignLabel = issues.createLabel(ownerToken, otherProject, "ops", "#10b981");

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Borrowed label","labelIds":["%s"]}""".formatted(foreignLabel)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("One or more labels do not belong to this project"));
    }

    @Test
    void projectViewersCanReadIssuesButNotCreateThem() throws Exception {
        var viewer = accounts.register("watcher", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, viewer.id(), Role.VIEWER);
        UUID issueId = issues.create(ownerToken, projectId, "Readable");

        mockMvc.perform(get("/api/issues/" + issueId).header("Authorization", "Bearer " + viewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("DEVF-1"));

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Not allowed"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/issues/" + issueId)
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hijacked","status":"DONE","priority":"LOW","type":"TASK"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMembersCannotReachIssuesAtAll() throws Exception {
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        UUID issueId = issues.create(ownerToken, projectId, "Private");

        mockMvc.perform(get("/api/issues/" + issueId).header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/issues/key/DEVF-1").header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void issuesCanBeFetchedByKeyAndUnknownKeysAreDenied() throws Exception {
        issues.create(ownerToken, projectId, "Findable");

        mockMvc.perform(get("/api/issues/key/devf-1").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Findable"));

        mockMvc.perform(get("/api/issues/key/DEVF-999").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/issues/key/NOTAKEY").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatingReplacesEveryEditableFieldIncludingOmittedOnes() throws Exception {
        var member = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, member.id(), Role.DEVELOPER);
        UUID label = issues.createLabel(ownerToken, projectId, "backend", "#6366f1");
        UUID issueId = issues.createFrom(ownerToken, projectId, """
                {"title":"Before","assigneeId":"%s","labelIds":["%s"],"estimatePoints":5}"""
                .formatted(member.id(), label));

        mockMvc.perform(put("/api/issues/" + issueId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"After","status":"IN_REVIEW","priority":"CRITICAL","type":"FEATURE"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("After"))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.priority").value("CRITICAL"))
                .andExpect(jsonPath("$.assignee").doesNotExist())
                .andExpect(jsonPath("$.estimatePoints").doesNotExist())
                .andExpect(jsonPath("$.labels").isEmpty());
    }

    @Test
    void changingStatusThroughAnEditMovesTheCardToTheEndOfItsNewColumn() throws Exception {
        issues.createFrom(ownerToken, projectId, """
                {"title":"Already in progress","status":"IN_PROGRESS"}""");
        UUID moved = issues.create(ownerToken, projectId, "Moving over");

        mockMvc.perform(put("/api/issues/" + moved)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Moving over","status":"IN_PROGRESS","priority":"MEDIUM","type":"TASK"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardPosition").value(2000.0));
    }

    @Test
    void onlyTheReporterOrAProjectAdminCanDeleteAnIssue() throws Exception {
        var author = accounts.register("author", Role.DEVELOPER);
        var other = accounts.register("other", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, author.id(), Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, other.id(), Role.DEVELOPER);
        UUID issueId = issues.create(author.token(), projectId, "Theirs to delete");

        mockMvc.perform(delete("/api/issues/" + issueId).header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/issues/" + issueId).header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isNoContent());

        assertThat(issueRepository.findById(issueId)).isEmpty();
    }

    @Test
    void projectAdminsCanDeleteAnyoneElsesIssue() throws Exception {
        var author = accounts.register("author", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, author.id(), Role.DEVELOPER);
        UUID issueId = issues.create(author.token(), projectId, "Someone else's");

        mockMvc.perform(delete("/api/issues/" + issueId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void issuesCannotBeTouchedOnAnArchivedProject() throws Exception {
        UUID issueId = issues.create(ownerToken, projectId, "Frozen");
        projects.archive(ownerToken, projectId, "DevFlow");

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Too late"}"""))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(put("/api/issues/" + issueId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Too late","status":"DONE","priority":"LOW","type":"TASK"}"""))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(get("/api/issues/" + issueId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void invalidIssuePayloadsAreRejected() throws Exception {
        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));

        mockMvc.perform(post("/api/projects/" + projectId + "/issues")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Too many points","estimatePoints":500}"""))
                .andExpect(status().isBadRequest());
    }
}
