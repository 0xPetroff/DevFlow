package com.devflow.deployment.controller;

import com.devflow.deployment.dto.EnvironmentRequest;
import com.devflow.deployment.dto.EnvironmentResponse;
import com.devflow.deployment.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/environments")
@Tag(name = "Environments", description = "Deployment targets belonging to a project")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @GetMapping
    @PreAuthorize("@projectAccess.canRead(#projectId)")
    @Operation(summary = "List a project's environments")
    public List<EnvironmentResponse> list(@PathVariable UUID projectId) {
        return environmentService.list(projectId);
    }

    @PostMapping
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add an environment. Administrators only: this is release configuration.")
    public EnvironmentResponse create(@PathVariable UUID projectId,
                                      @Valid @RequestBody EnvironmentRequest request) {
        return environmentService.create(projectId, request);
    }

    @PutMapping("/{environmentId}")
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @Operation(summary = "Update an environment, including whether releases to it need approval")
    public EnvironmentResponse update(@PathVariable UUID projectId, @PathVariable UUID environmentId,
                                      @Valid @RequestBody EnvironmentRequest request) {
        return environmentService.update(projectId, environmentId, request);
    }

    @DeleteMapping("/{environmentId}")
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an environment that has never been deployed to")
    public void delete(@PathVariable UUID projectId, @PathVariable UUID environmentId) {
        environmentService.delete(projectId, environmentId);
    }
}
