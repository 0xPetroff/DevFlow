package com.devflow.project.controller;

import com.devflow.project.dto.AddMemberRequest;
import com.devflow.project.dto.ProjectMemberResponse;
import com.devflow.project.dto.UpdateMemberRoleRequest;
import com.devflow.project.service.ProjectMemberService;
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
@RequestMapping("/api/projects/{projectId}/members")
@Tag(name = "Project members", description = "Per-project role assignment")
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    public ProjectMemberController(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @PreAuthorize("@projectAccess.canRead(#projectId)")
    @Operation(summary = "List everyone with access to the project")
    public List<ProjectMemberResponse> list(@PathVariable UUID projectId) {
        return memberService.list(projectId);
    }

    @PostMapping
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Grant a user a role on the project")
    public ProjectMemberResponse add(@PathVariable UUID projectId,
                                     @Valid @RequestBody AddMemberRequest request) {
        return memberService.add(projectId, request);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @Operation(summary = "Change a member's role on the project")
    public ProjectMemberResponse changeRole(@PathVariable UUID projectId,
                                            @PathVariable UUID userId,
                                            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return memberService.changeRole(projectId, userId, request);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a user's access to the project")
    public void remove(@PathVariable UUID projectId, @PathVariable UUID userId) {
        memberService.remove(projectId, userId);
    }
}
