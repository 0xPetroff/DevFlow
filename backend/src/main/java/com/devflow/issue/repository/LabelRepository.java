package com.devflow.issue.repository;

import com.devflow.issue.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    List<Label> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<Label> findByIdAndProjectId(UUID id, UUID projectId);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    /** Filtered by project as well as id so labels from another project can never be attached. */
    List<Label> findByProjectIdAndIdIn(UUID projectId, Collection<UUID> ids);
}
