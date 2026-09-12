package com.devflow.apikey.service;

import com.devflow.apikey.dto.ApiKeyResponse;
import com.devflow.apikey.dto.CreateApiKeyRequest;
import com.devflow.apikey.dto.IssuedApiKeyResponse;
import com.devflow.apikey.entity.ApiKey;
import com.devflow.apikey.mapper.ApiKeyMapper;
import com.devflow.apikey.repository.ApiKeyRepository;
import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.common.Sha256;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.project.entity.Project;
import com.devflow.project.service.ProjectService;
import com.devflow.security.ApiKeyPrincipal;
import com.devflow.user.entity.User;
import com.devflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ApiKeyService {

    private static final String KEY_PREFIX = "dfk";
    /*
     * Hex rather than base64url for both halves. The key is read back as three underscore-separated
     * parts, and the base64url alphabet contains underscores of its own, so a key could not be
     * parsed reliably; hex has no character in common with the separator.
     */
    private static final int PREFIX_BYTES = 6;
    private static final int SECRET_BYTES = 32;

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final ApiKeyMapper apiKeyMapper;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository,
                         UserRepository userRepository,
                         ProjectService projectService,
                         ApiKeyMapper apiKeyMapper,
                         AuditService auditService) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
        this.apiKeyMapper = apiKeyMapper;
        this.auditService = auditService;
    }

    /**
     * The only moment the key exists in readable form. Everything after this point works from the
     * hash, so a lost key is reissued rather than recovered.
     */
    @Transactional
    public IssuedApiKeyResponse create(UUID projectId, CreateApiKeyRequest request, UUID actorId) {
        Project project = projectService.requireProject(projectId);
        User creator = userRepository.findById(actorId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", actorId));

        String prefix = randomHex(PREFIX_BYTES);
        String raw = "%s_%s_%s".formatted(KEY_PREFIX, prefix, randomHex(SECRET_BYTES));
        Instant expiresAt = request.expiresInDays() == null
                ? null
                : Instant.now().plus(request.expiresInDays(), ChronoUnit.DAYS);

        ApiKey key = apiKeyRepository.save(new ApiKey(request.name().trim(), prefix,
                Sha256.hex(raw), project, creator, expiresAt));

        auditService.record(AuditAction.API_KEY_CREATED, "ApiKey", key.getId(), projectId,
                "Issued API key %s for %s".formatted(key.getName(), project.getProjectKey()),
                Map.of("keyPrefix", prefix, "scopes", key.getScopes()));

        return new IssuedApiKeyResponse(apiKeyMapper.toResponse(key), raw);
    }

    public List<ApiKeyResponse> list(UUID projectId) {
        projectService.requireProject(projectId);
        return apiKeyRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(apiKeyMapper::toResponse)
                .toList();
    }

    /** Revoked rather than deleted: the audit trail should still be able to name the key. */
    @Transactional
    public void revoke(UUID projectId, UUID keyId) {
        ApiKey key = apiKeyRepository.findByIdAndProjectId(keyId, projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("ApiKey", keyId));
        key.revoke();

        auditService.record(AuditAction.API_KEY_REVOKED, "ApiKey", key.getId(), projectId,
                "Revoked API key %s".formatted(key.getName()), Map.of("keyPrefix", key.getKeyPrefix()));
    }

    /**
     * Looks a presented key up by its prefix and confirms it by hash. The comparison is
     * constant-time so that a caller cannot learn a valid hash from how long the check took.
     */
    @Transactional
    public Optional<ApiKeyPrincipal> authenticate(String presented) {
        String[] parts = presented.split("_");
        if (parts.length != 3 || !KEY_PREFIX.equals(parts[0])) {
            return Optional.empty();
        }

        return apiKeyRepository.findByKeyPrefix(parts[1])
                .filter(key -> matches(key, presented))
                .filter(ApiKey::isActive)
                .map(key -> {
                    key.recordUse();
                    return new ApiKeyPrincipal(key.getId(), key.getName(),
                            key.getProject() == null ? null : key.getProject().getId(),
                            key.scopeSet());
                });
    }

    private boolean matches(ApiKey key, String presented) {
        return MessageDigest.isEqual(key.getKeyHash().getBytes(StandardCharsets.UTF_8),
                Sha256.hex(presented).getBytes(StandardCharsets.UTF_8));
    }

    private String randomHex(int bytes) {
        byte[] buffer = new byte[bytes];
        random.nextBytes(buffer);
        return HexFormat.of().formatHex(buffer);
    }
}
