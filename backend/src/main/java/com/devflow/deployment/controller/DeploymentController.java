package com.devflow.deployment.controller;

import com.devflow.deployment.dto.CreateDeploymentRequest;
import com.devflow.deployment.dto.DeploymentResponse;
import com.devflow.deployment.dto.DeploymentStatusRequest;
import com.devflow.deployment.service.DeploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * The endpoints a CI pipeline talks to, and the only ones that accept an API key. Both security
 * schemes are declared on each operation so the Swagger page offers either credential.
 */
@RestController
@RequestMapping("/api/deployments")
@Tag(name = "Deployments", description = "Release history and the deployment state machine")
@SecurityRequirements({
        @SecurityRequirement(name = "bearerAuth"),
        @SecurityRequirement(name = "apiKeyAuth")
})
public class DeploymentController {

    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping
    @PreAuthorize("@deploymentAccess.canWrite(#request.projectId())")
    @Operation(summary = "Record a deployment, which starts out PENDING",
            description = "Called by a pipeline with an API key, or by a person with write access to the project.")
    public ResponseEntity<DeploymentResponse> create(@Valid @RequestBody CreateDeploymentRequest request) {
        DeploymentResponse created = deploymentService.create(request);
        return ResponseEntity.created(URI.create("/api/deployments/" + created.id())).body(created);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@deploymentAccess.canReadDeployment(#id)")
    @Operation(summary = "Fetch a single deployment")
    public DeploymentResponse get(@PathVariable UUID id) {
        return deploymentService.getById(id);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@deploymentAccess.canWriteDeployment(#id)")
    @Operation(summary = "Advance the deployment's status",
            description = """
                    PENDING moves to RUNNING or CANCELLED; RUNNING moves to SUCCESS, FAILED or
                    CANCELLED. A finished deployment cannot be reopened. Starting a release to an
                    environment that requires approval is reserved for project administrators.""")
    public DeploymentResponse transition(@PathVariable UUID id,
                                         @Valid @RequestBody DeploymentStatusRequest request) {
        return deploymentService.transition(id, request);
    }
}
