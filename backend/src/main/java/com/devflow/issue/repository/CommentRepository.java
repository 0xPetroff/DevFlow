package com.devflow.issue.repository;

import com.devflow.issue.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @EntityGraph(attributePaths = "author")
    Page<Comment> findByIssueIdOrderByCreatedAtAsc(UUID issueId, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Comment> findByIdAndIssueId(UUID id, UUID issueId);
}
