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

    List<Issue> findByProjectIdAndStatusOrderByBoardPositionAsc(UUID projectId, IssueStatus status);

    /**
     * The board's four column totals in one round trip. Counting per status cost a query each,
     * which on a shared-CPU instance is four times the fixed overhead to fetch four numbers.
     */
    @Query("""
            select i.status as status, count(i) as total
            from Issue i
            where i.project.id = :projectId
            group by i.status""")
    List<StatusCount> countByStatusForProject(@Param("projectId") UUID projectId);

    /**
     * The ids of the cards the board shows: every column ranked by board position and cut at the
     * same limit, in one query rather than one per column.
     *
     * <p>Native because the cap is per column rather than over the result, which needs a window
     * function inside a derived table, and HQL has no from-clause subquery to put one in. It
     * selects ids alone so that fetching the rows stays a separate query, free to join-fetch
     * without the window function constraining what it can reach. Covered by idx_issues_board.
     */
    @Query(value = """
            select ranked.id
            from (select i.id as id,
                         row_number() over (partition by i.status order by i.board_position asc) as rn
                  from issues i
                  where i.project_id = :projectId) ranked
            where ranked.rn <= :limitPerColumn""", nativeQuery = true)
    List<UUID> findBoardIssueIds(@Param("projectId") UUID projectId,
                                 @Param("limitPerColumn") int limitPerColumn);

    /**
     * Labels are join-fetched here where the paginated finders leave them to @BatchSize. The
     * difference is the pagination: this loads a fixed set of ids, so there is no limit for the
     * collection join to be applied in memory against, and the whole board's labels arrive with
     * the cards rather than as a batch per column.
     */
    @EntityGraph(attributePaths = {"project", "assignee", "creator", "labels"})
    List<Issue> findByIdIn(Collection<UUID> ids);

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
