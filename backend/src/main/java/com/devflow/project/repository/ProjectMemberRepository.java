package com.devflow.project.repository;

import com.devflow.project.entity.ProjectMember;
import com.devflow.user.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findByProjectIdOrderByCreatedAtAsc(UUID projectId);

    long countByProjectId(UUID projectId);

    /** Returns the role alone so a permission check never loads the member, its project and its user. */
    @Query("select pm.role from ProjectMember pm where pm.project.id = :projectId and pm.user.id = :userId")
    Optional<Role> findRole(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    /** One grouped count for a whole page of projects instead of a count query per row. */
    @Query("""
            select pm.project.id as projectId, count(pm) as total
            from ProjectMember pm
            where pm.project.id in :projectIds
            group by pm.project.id""")
    List<MemberCount> countByProjectIds(@Param("projectIds") Collection<UUID> projectIds);

    interface MemberCount {
        UUID getProjectId();

        long getTotal();
    }
}
