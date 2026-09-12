package com.devflow.deployment.repository;

import com.devflow.deployment.entity.Deployment;
import com.devflow.deployment.entity.DeploymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeploymentRepository extends JpaRepository<Deployment, UUID>,
        JpaSpecificationExecutor<Deployment> {

    @Override
    @EntityGraph(attributePaths = {"project", "environment", "triggeredBy"})
    Page<Deployment> findAll(Specification<Deployment> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"project", "environment", "triggeredBy"})
    Optional<Deployment> findById(UUID id);

    boolean existsByEnvironmentId(UUID environmentId);

    @Query("select d.project.id from Deployment d where d.id = :deploymentId")
    Optional<UUID> findProjectId(@Param("deploymentId") UUID deploymentId);

    @EntityGraph(attributePaths = {"project", "environment", "triggeredBy"})
    List<Deployment> findByProjectIdInOrderByQueuedAtDesc(Collection<UUID> projectIds, Pageable pageable);

    @Query("""
            select d.status as status, count(d) as total
            from Deployment d
            where d.project.id in :projectIds and d.queuedAt >= :since
            group by d.status""")
    List<StatusCount> countByStatusSince(@Param("projectIds") Collection<UUID> projectIds,
                                         @Param("since") Instant since);

    interface StatusCount {
        DeploymentStatus getStatus();

        long getTotal();
    }
}
