package com.devflow.project.controller;

import com.devflow.common.PageResponse;
import com.devflow.common.SortProperties;
import com.devflow.project.dto.CreateProjectRequest;
import com.devflow.project.dto.ProjectResponse;
import com.devflow.project.dto.UpdateProjectRequest;
import com.devflow.project.entity.ProjectStatus;
import com.devflow.project.service.ProjectService;
import com.devflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Projects", description = "Project lifecycle and per-project access")
public class ProjectController {

    private static final Set<String> SORTABLE = Set.of("name", "projectKey", "status", "createdAt", "updatedAt");

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEVELOPER')")
    @Operation(summary = "Create a project. The caller becomes its owner and first administrator.")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        ProjectResponse created = projectService.create(request, principal.getId());
        return ResponseEntity.created(URI.create("/api/projects/" + created.id())).body(created);
    }

    @GetMapping
    @Operation(summary = "List the projects the caller can see, filtered and paginated")
    public PageResponse<ProjectResponse> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) ProjectStatus status,
                                              @AuthenticationPrincipal UserPrincipal principal,
                                              @PageableDefault(size = 20, sort = "name",
                                                      direction = Sort.Direction.ASC) Pageable pageable) {
        return projectService.search(q, status, principal, SortProperties.validate(pageable, SORTABLE));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectAccess.canRead(#id)")
    @Operation(summary = "Fetch a single project")
    public ProjectResponse get(@PathVariable UUID id) {
        return projectService.getById(id);
    }

    @GetMapping("/key/{projectKey}")
    @Operation(summary = "Fetch a project by its key, for example DEVF")
    public ProjectResponse getByKey(@PathVariable String projectKey) {
        return projectService.getByKey(projectKey);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@projectAccess.canAdmin(#id)")
    @Operation(summary = "Update a project's details, status or owner")
    public ProjectResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectAccess.canAdmin(#id)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an archived project and everything inside it")
    public void delete(@PathVariable UUID id) {
        projectService.delete(id);
    }
}
