package com.devflow.apikey.controller;

import com.devflow.apikey.dto.ApiKeyResponse;
import com.devflow.apikey.dto.CreateApiKeyRequest;
import com.devflow.apikey.dto.IssuedApiKeyResponse;
import com.devflow.apikey.service.ApiKeyService;
import com.devflow.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/api-keys")
@Tag(name = "API keys", description = "Project-scoped credentials for build agents")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    public ApiKeyController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @Operation(summary = "List a project's API keys. The keys themselves are never returned again.")
    public List<ApiKeyResponse> list(@PathVariable UUID projectId) {
        return apiKeyService.list(projectId);
    }

    @PostMapping
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Issue an API key. The response is the only place the key is ever shown.")
    public IssuedApiKeyResponse create(@PathVariable UUID projectId,
                                       @Valid @RequestBody CreateApiKeyRequest request,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return apiKeyService.create(projectId, request, principal.getId());
    }

    @DeleteMapping("/{keyId}")
    @PreAuthorize("@projectAccess.canAdmin(#projectId)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a key. The record is kept so the audit trail can still name it.")
    public void revoke(@PathVariable UUID projectId, @PathVariable UUID keyId) {
        apiKeyService.revoke(projectId, keyId);
    }
}
