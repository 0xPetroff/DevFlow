package com.devflow.issue.repository;

import com.devflow.issue.entity.Issue;
import com.devflow.issue.entity.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends JpaRepository<Issue, UUID>, JpaSpecificationExecutor<Issue> {

    /*
     * Only the to-one associations are join-fetched. Adding labels here would combine a collection
     * fetch with pagination, which makes Hibernate page in memory; @BatchSize on the collection
     * loads them in a couple of extra queries instead.
     */
    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "creator"})
    Page<Issue> findAll(Specification<Issue> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"project", "assignee", "creator", "labels"})
    Optional<Issue> findById(UUID id);

    @EntityGraph(attributePaths = {"project", "assignee", "creator", "labels"})
    Optional<Issue> findByProjectIdAndIssueNumber(UUID projectId, int issueNumber);

    @EntityGraph(attributePaths = {"project", "assignee", "creator"})
    List<Issue> findByProjectIdAndStatusOrderByBoardPositionAsc(UUID projectId, IssueStatus status,
                                                                Pageable pageable);

    List<Issue> findByProjectIdAndStatusOrderByBoardPositionAsc(UUID projectId, IssueStatus status);

    long countByProjectIdAndStatus(UUID projectId, IssueStatus status);

    @Query("select max(i.boardPosition) from Issue i where i.project.id = :projectId and i.status = :status")
    Optional<Double> findLastPosition(@Param("projectId") UUID projectId, @Param("status") IssueStatus status);

    @Query("select i.project.id from Issue i where i.id = :issueId")
    Optional<UUID> findProjectId(@Param("issueId") UUID issueId);

    @Query("""
            select i.status as status, count(i) as total
            from Issue i
            where i.project.id in :projectIds
            group by i.status""")
    List<StatusCount> countByStatus(@Param("projectIds") Collection<UUID> projectIds);

    long countByProjectIdInAndAssigneeId(Collection<UUID> projectIds, UUID assigneeId);

    long countByProjectIdInAndDueDateBeforeAndStatusNot(Collection<UUID> projectIds, LocalDate date,
                                                        IssueStatus status);

    interface StatusCount {
        IssueStatus getStatus();

        long getTotal();
    }
}
