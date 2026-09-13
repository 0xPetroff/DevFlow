package com.devflow.project.repository;

import com.devflow.project.entity.Project;
import com.devflow.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    /** The graph is what stops the lazy owner association from firing one query per row in a listing. */
    @Override
    @EntityGraph(attributePaths = "owner")
    Page<Project> findAll(Specification<Project> specification, Pageable pageable);

    Optional<Project> findByProjectKey(String projectKey);

    boolean existsByProjectKey(String projectKey);

    boolean existsByIdAndOwnerId(UUID id, UUID ownerId);

    @Query("select p.id from Project p")
    List<UUID> findAllIds();

    /** The id-only twin of ProjectSpecifications.visibleTo, for aggregations that only need scope. */
    @Query("""
            select p.id from Project p
            where p.owner.id = :userId
               or exists (select 1 from ProjectMember m where m.project = p and m.user.id = :userId)""")
    List<UUID> findVisibleIds(@Param("userId") UUID userId);

    long countByIdInAndStatus(Collection<UUID> ids, ProjectStatus status);

    /**
     * Claims the next issue number for a project in a single statement. The row lock PostgreSQL
     * takes for the UPDATE is what serialises concurrent creates, so two issues can never be
     * handed the same number; read-then-write in application code could not promise that.
     *
     * <p>Project.issueSequence stays read-only in the mapping so Hibernate never writes a stale
     * copy of the counter back over the value claimed here.
     */
    @Query(value = """
            UPDATE projects SET issue_sequence = issue_sequence + 1
            WHERE id = :projectId
            RETURNING issue_sequence""", nativeQuery = true)
    int claimNextIssueNumber(@Param("projectId") UUID projectId);
}
