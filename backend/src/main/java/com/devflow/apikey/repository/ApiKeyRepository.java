package com.devflow.apikey.repository;

import com.devflow.apikey.entity.ApiKey;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /** The prefix is the public, indexed handle; the hash of the whole key is what proves it. */
    @EntityGraph(attributePaths = "project")
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    @EntityGraph(attributePaths = "createdBy")
    List<ApiKey> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    Optional<ApiKey> findByIdAndProjectId(UUID id, UUID projectId);
}
