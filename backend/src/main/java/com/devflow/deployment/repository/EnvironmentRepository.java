package com.devflow.deployment.repository;

import com.devflow.deployment.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnvironmentRepository extends JpaRepository<Environment, UUID> {

    List<Environment> findByProjectIdOrderByNameAsc(UUID projectId);

    Optional<Environment> findByIdAndProjectId(UUID id, UUID projectId);

    Optional<Environment> findByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);
}
