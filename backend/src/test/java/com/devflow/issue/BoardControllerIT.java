package com.devflow.issue;

import com.devflow.issue.entity.Issue;
import com.devflow.issue.repository.IssueRepository;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.support.TestIssues;
import com.devflow.support.TestProjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class BoardControllerIT {

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

    private String token;
    private UUID projectId;

    @BeforeEach
    void setUp() throws Exception {
        accounts.deleteEverything();
        token = accounts.registerAdmin("boss");
        projectId = projects.create(token, "DEVF", "DevFlow");
    }

    @Test
    void theBoardReturnsEveryColumnInPositionOrder() throws Exception {
        issues.create(token, projectId, "First");
        issues.create(token, projectId, "Second");
        issues.createFrom(token, projectId, """
                {"title":"Underway","status":"IN_PROGRESS"}""");

        mockMvc.perform(get("/api/projects/" + projectId + "/board")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.project.projectKey").value("DEVF"))
                .andExpect(jsonPath("$.columns.length()").value(4))
                .andExpect(jsonPath("$.columns[0].status").value("TODO"))
                .andExpect(jsonPath("$.columns[0].total").value(2))
                .andExpect(jsonPath("$.columns[0].issues[0].title").value("First"))
                .andExpect(jsonPath("$.columns[0].issues[1].title").value("Second"))
                .andExpect(jsonPath("$.columns[1].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.columns[1].total").value(1))
                .andExpect(jsonPath("$.columns[3].status").value("DONE"))
                .andExpect(jsonPath("$.columns[3].total").value(0))
                .andExpect(jsonPath("$.columns[3].issues").isEmpty());
    }

    @Test
    void droppingACardBetweenTwoOthersSplitsTheGap() throws Exception {
        UUID first = issues.create(token, projectId, "First");
        UUID second = issues.create(token, projectId, "Second");
        UUID third = issues.create(token, projectId, "Third");

        move(third, """
                {"status":"TODO","previousIssueId":"%s","nextIssueId":"%s"}""".formatted(first, second))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardPosition").value(1500.0));

        assertColumnOrder("TODO", "First", "Third", "Second");
    }

    @Test
    void droppingACardAtTheTopOfAColumnHalvesTheFirstPosition() throws Exception {
        UUID first = issues.create(token, projectId, "First");
        UUID second = issues.create(token, projectId, "Second");

        move(second, """
                {"status":"TODO","nextIssueId":"%s"}""".formatted(first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardPosition").value(500.0));

        assertColumnOrder("TODO", "Second", "First");
    }

    @Test
    void droppingACardIntoAnEmptyColumnChangesItsStatus() throws Exception {
        UUID issueId = issues.create(token, projectId, "Moving");

        move(issueId, """
                {"status":"DONE"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.boardPosition").value(1000.0));

        mockMvc.perform(get("/api/projects/" + projectId + "/board")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.columns[0].total").value(0))
                .andExpect(jsonPath("$.columns[3].total").value(1));
    }

    @Test
    void neighboursOutsideTheTargetColumnAreRejected() throws Exception {
        UUID inTodo = issues.create(token, projectId, "In todo");
        UUID moving = issues.create(token, projectId, "Moving");

        move(moving, """
                {"status":"DONE","previousIssueId":"%s"}""".formatted(inTodo))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("The neighbouring cards must be in the target column"));
    }

    @Test
    void aCardCannotBePositionedAgainstItself() throws Exception {
        UUID issueId = issues.create(token, projectId, "Lonely");

        move(issueId, """
                {"status":"TODO","previousIssueId":"%s"}""".formatted(issueId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("An issue cannot be positioned against itself"));
    }

    @Test
    void neighboursGivenInTheWrongOrderAreRejected() throws Exception {
        UUID first = issues.create(token, projectId, "First");
        UUID second = issues.create(token, projectId, "Second");
        UUID third = issues.create(token, projectId, "Third");

        move(third, """
                {"status":"TODO","previousIssueId":"%s","nextIssueId":"%s"}""".formatted(second, first))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("The neighbouring cards are not in board order"));
    }

    /**
     * Sparse ordering only works while there is room between neighbours. Once the gap is too small
     * to split, the column has to be renumbered or the dropped card lands on top of a neighbour.
     */
    @Test
    void anUnsplittableGapRenumbersTheColumn() throws Exception {
        UUID first = issues.create(token, projectId, "First");
        UUID second = issues.create(token, projectId, "Second");
        UUID third = issues.create(token, projectId, "Third");
        setPosition(second, 1000.00001);

        move(third, """
                {"status":"TODO","previousIssueId":"%s","nextIssueId":"%s"}""".formatted(first, second))
                .andExpect(status().isOk());

        assertColumnOrder("TODO", "First", "Third", "Second");
        assertThat(issueRepository.findById(first).orElseThrow().getBoardPosition()).isEqualTo(1000.0);
        assertThat(issueRepository.findById(second).orElseThrow().getBoardPosition()).isEqualTo(2000.0);
        assertThat(issueRepository.findById(third).orElseThrow().getBoardPosition()).isEqualTo(1500.0);
    }

    @Test
    void theBoardCapsAColumnButStillReportsItsTotal() throws Exception {
        for (int i = 0; i < 3; i++) {
            issues.create(token, projectId, "Card " + i);
        }

        mockMvc.perform(get("/api/projects/" + projectId + "/board?limitPerColumn=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0].total").value(3))
                .andExpect(jsonPath("$.columns[0].issues.length()").value(2));
    }

    private ResultActions move(UUID issueId, String body) throws Exception {
        return mockMvc.perform(put("/api/issues/" + issueId + "/position")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private void setPosition(UUID issueId, double position) {
        Issue issue = issueRepository.findById(issueId).orElseThrow();
        issue.setBoardPosition(position);
        issueRepository.saveAndFlush(issue);
    }

    private void assertColumnOrder(String status, String... titles) throws Exception {
        var result = mockMvc.perform(get("/api/projects/" + projectId + "/board")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        int column = List.of("TODO", "IN_PROGRESS", "IN_REVIEW", "DONE").indexOf(status);
        for (int i = 0; i < titles.length; i++) {
            result.andExpect(jsonPath("$.columns[%d].issues[%d].title".formatted(column, i)).value(titles[i]));
        }
    }
}
