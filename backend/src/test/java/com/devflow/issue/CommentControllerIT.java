package com.devflow.issue;

import com.devflow.issue.repository.CommentRepository;
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
class CommentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestAccounts accounts;

    @Autowired
    private TestProjects projects;

    @Autowired
    private TestIssues issues;

    private String ownerToken;
    private UUID projectId;
    private UUID issueId;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        ownerToken = accounts.registerAdmin("boss");
        projectId = projects.create(ownerToken, "DEVF", "DevFlow");
        issueId = issues.create(ownerToken, projectId, "Needs discussion");
    }

    @Test
    void commentsAreAddedAndListedOldestFirst() throws Exception {
        mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Looking into it"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Looking into it"))
                .andExpect(jsonPath("$.author.username").value("boss"))
                .andExpect(jsonPath("$.edited").value(false));

        issues.comment(ownerToken, issueId, "And another");

        mockMvc.perform(get("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].body").value("Looking into it"))
                .andExpect(jsonPath("$.content[1].body").value("And another"));
    }

    @Test
    void onlyTheAuthorCanEditTheirComment() throws Exception {
        var member = accounts.register("dev", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, member.id(), Role.DEVELOPER);
        UUID commentId = issues.comment(member.token(), issueId, "Mine");

        mockMvc.perform(put("/api/issues/" + issueId + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Rewritten by someone else"}"""))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/issues/" + issueId + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Rewritten by me"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Rewritten by me"))
                .andExpect(jsonPath("$.edited").value(true));
    }

    @Test
    void authorsAndProjectAdminsCanDeleteButOtherMembersCannot() throws Exception {
        var author = accounts.register("author", Role.DEVELOPER);
        var other = accounts.register("other", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, author.id(), Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, other.id(), Role.DEVELOPER);

        UUID theirs = issues.comment(author.token(), issueId, "Theirs");
        mockMvc.perform(delete("/api/issues/" + issueId + "/comments/" + theirs)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/issues/" + issueId + "/comments/" + theirs)
                        .header("Authorization", "Bearer " + author.token()))
                .andExpect(status().isNoContent());

        UUID moderated = issues.comment(author.token(), issueId, "Also theirs");
        mockMvc.perform(delete("/api/issues/" + issueId + "/comments/" + moderated)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        assertThat(commentRepository.count()).isZero();
    }

    @Test
    void projectViewersCanReadCommentsButNotWriteThem() throws Exception {
        var viewer = accounts.register("watcher", Role.DEVELOPER);
        projects.addMember(ownerToken, projectId, viewer.id(), Role.VIEWER);
        issues.comment(ownerToken, issueId, "Visible");

        mockMvc.perform(get("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + viewer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + viewer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Not allowed"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonMembersCannotSeeTheDiscussion() throws Exception {
        var outsider = accounts.register("outsider", Role.DEVELOPER);
        issues.comment(ownerToken, issueId, "Private");

        mockMvc.perform(get("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + outsider.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void commentsOfAnotherIssueAreNotReachableThroughThisOne() throws Exception {
        UUID otherIssue = issues.create(ownerToken, projectId, "Another issue");
        UUID commentId = issues.comment(ownerToken, otherIssue, "Belongs elsewhere");

        mockMvc.perform(delete("/api/issues/" + issueId + "/comments/" + commentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void commentingIsBlockedOnAnArchivedProject() throws Exception {
        projects.archive(ownerToken, projectId, "DevFlow");

        mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Too late"}"""))
                .andExpect(status().isUnprocessableContent());

        mockMvc.perform(get("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void emptyCommentsAreRejected() throws Exception {
        mockMvc.perform(post("/api/issues/" + issueId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"   "}"""))
                .andExpect(status().isBadRequest());
    }
}
