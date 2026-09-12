package com.devflow.issue.controller;

import com.devflow.issue.dto.LabelRequest;
import com.devflow.issue.dto.LabelResponse;
import com.devflow.issue.service.LabelService;
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
@RequestMapping("/api/projects/{projectId}/labels")
@Tag(name = "Labels", description = "Per-project issue labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    @PreAuthorize("@projectAccess.canRead(#projectId)")
    @Operation(summary = "List a project's labels")
    public List<LabelResponse> list(@PathVariable UUID projectId) {
        return labelService.list(projectId);
    }

    @PostMapping
    @PreAuthorize("@projectAccess.canWrite(#projectId)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a label")
    public LabelResponse create(@PathVariable UUID projectId, @Valid @RequestBody LabelRequest request) {
        return labelService.create(projectId, request);
    }

    @PutMapping("/{labelId}")
    @PreAuthorize("@projectAccess.canWrite(#projectId)")
    @Operation(summary = "Rename or recolour a label")
    public LabelResponse update(@PathVariable UUID projectId, @PathVariable UUID labelId,
                                @Valid @RequestBody LabelRequest request) {
        return labelService.update(projectId, labelId, request);
    }

    @DeleteMapping("/{labelId}")
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a label and remove it from every issue that carries it")
    public void delete(@PathVariable UUID projectId, @PathVariable UUID labelId) {
        labelService.delete(projectId, labelId);
    }
}
