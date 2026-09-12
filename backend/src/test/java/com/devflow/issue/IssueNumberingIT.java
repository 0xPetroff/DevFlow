package com.devflow.issue;

import com.devflow.issue.dto.CreateIssueRequest;
import com.devflow.issue.dto.IssueResponse;
import com.devflow.issue.service.IssueService;
import com.devflow.project.entity.Project;
import com.devflow.project.entity.ProjectMember;
import com.devflow.project.repository.ProjectMemberRepository;
import com.devflow.project.repository.ProjectRepository;
import com.devflow.support.IntegrationTest;
import com.devflow.support.TestAccounts;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue numbers come from an UPDATE ... RETURNING on the project row. This proves the claim that
 * makes them safe: parallel creates serialise on that row and never share a number.
 */
@IntegrationTest
class IssueNumberingIT {

    private static final int PARALLEL_CREATES = 8;

    @Autowired
    private IssueService issueService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestAccounts accounts;

    private UUID projectId;
    private UUID creatorId;

    @BeforeEach
    void setUp() {
        accounts.deleteEverything();
        User owner = userRepository.saveAndFlush(
                new User("owner@example.com", "owner", "not-a-real-hash", "Owner", Role.DEVELOPER));
        Project project = projectRepository.saveAndFlush(new Project("DEVF", "DevFlow", owner));
        memberRepository.saveAndFlush(new ProjectMember(project, owner, Role.ADMIN));
        projectId = project.getId();
        creatorId = owner.getId();
    }

    @Test
    void numbersRunFromOneInCreationOrder() {
        List<Integer> numbers = List.of(create("First"), create("Second"), create("Third"));

        assertThat(numbers).containsExactly(1, 2, 3);
    }

    @Test
    void keysAreScopedToTheirProject() {
        User other = userRepository.saveAndFlush(
                new User("other@example.com", "other", "not-a-real-hash", "Other", Role.DEVELOPER));
        Project second = projectRepository.saveAndFlush(new Project("OPS", "Operations", other));
        memberRepository.saveAndFlush(new ProjectMember(second, other, Role.ADMIN));

        IssueResponse first = issueService.create(projectId, request("In DevFlow"), creatorId);
        IssueResponse secondIssue = issueService.create(second.getId(), request("In Operations"), other.getId());

        assertThat(first.key()).isEqualTo("DEVF-1");
        assertThat(secondIssue.key()).isEqualTo("OPS-1");
    }

    @Test
    void concurrentCreatesNeverShareANumber() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_CREATES);
        CountDownLatch release = new CountDownLatch(1);

        try {
            List<Future<Integer>> claims = new ArrayList<>();
            for (int i = 0; i < PARALLEL_CREATES; i++) {
                String title = "Concurrent " + i;
                claims.add(pool.submit(() -> {
                    release.await();
                    return issueService.create(projectId, request(title), creatorId).issueNumber();
                }));
            }

            release.countDown();
            List<Integer> numbers = new ArrayList<>();
            for (Future<Integer> claim : claims) {
                numbers.add(claim.get(30, TimeUnit.SECONDS));
            }

            assertThat(numbers).hasSize(PARALLEL_CREATES).doesNotHaveDuplicates()
                    .containsExactlyInAnyOrderElementsOf(
                            IntStream.rangeClosed(1, PARALLEL_CREATES).boxed().toList());
        } finally {
            pool.shutdownNow();
        }
    }

    private int create(String title) {
        return issueService.create(projectId, request(title), creatorId).issueNumber();
    }

    private CreateIssueRequest request(String title) {
        return new CreateIssueRequest(title, null, null, null, null, null, null, null, null);
    }
}
