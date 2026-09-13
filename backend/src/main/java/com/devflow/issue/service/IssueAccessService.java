package com.devflow.issue.service;

import com.devflow.issue.repository.IssueRepository;
import com.devflow.project.service.ProjectAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves an issue to its project so issue-scoped endpoints can keep using method security:
 * {@code @PreAuthorize("@issueAccess.canWrite(#issueId)")}. Permission itself still belongs to
 * ProjectAccessService; this only answers which project is being asked about.
 */
@Service("issueAccess")
@Transactional(readOnly = true)
public class IssueAccessService {

    private final IssueRepository issueRepository;
    private final ProjectAccessService projectAccess;

    public IssueAccessService(IssueRepository issueRepository, ProjectAccessService projectAccess) {
        this.issueRepository = issueRepository;
        this.projectAccess = projectAccess;
    }

    public boolean canRead(UUID issueId) {
        return projectOf(issueId).map(projectAccess::canRead).orElse(false);
    }

    public boolean canWrite(UUID issueId) {
        return projectOf(issueId).map(projectAccess::canWrite).orElse(false);
    }

    private Optional<UUID> projectOf(UUID issueId) {
        return issueId == null ? Optional.empty() : issueRepository.findProjectId(issueId);
    }
}
