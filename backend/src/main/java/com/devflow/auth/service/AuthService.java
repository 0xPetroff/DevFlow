package com.devflow.auth.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.auth.dto.AuthResponse;
import com.devflow.auth.dto.LoginRequest;
import com.devflow.auth.dto.RegisterRequest;
import com.devflow.auth.entity.RefreshToken;
import com.devflow.auth.repository.RefreshTokenRepository;
import com.devflow.exception.ConflictException;
import com.devflow.security.JwtService;
import com.devflow.security.UserPrincipal;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import com.devflow.user.mapper.UserMapper;
import com.devflow.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       AuthenticationManager authenticationManager,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       UserMapper userMapper,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("This username is already taken");
        }

        // The first account to register owns the instance; everyone after joins as a developer.
        Role role = userRepository.count() == 0 ? Role.ADMIN : Role.DEVELOPER;

        User user = new User(email, request.username(), passwordEncoder.encode(request.password()),
                request.fullName().trim(), role);
        userRepository.save(user);

        auditService.recordAs(user.getId(), user.getUsername(), AuditAction.USER_CREATED, "User",
                user.getId(), null, "%s registered".formatted(user.getUsername()),
                Map.of("role", role.name()));

        log.info("Registered user {} with role {}", user.getUsername(), role);
        return issueTokens(UserPrincipal.from(user), user, httpRequest);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.identifier(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        user.recordLogin();

        auditService.recordAs(user.getId(), user.getUsername(), AuditAction.USER_LOGGED_IN, "User",
                user.getId(), null, "%s signed in".formatted(user.getUsername()), Map.of());

        return issueTokens(principal, user, httpRequest);
    }

    /**
     * Rotates the presented token: the old row is revoked and linked to its replacement.
     * Presenting an already-revoked token revokes the whole family, on the assumption that
     * a consumed token in circulation means it was stolen.
     */
    /*
     * noRollbackFor is essential, not incidental: the theft-detection branch below revokes the
     * token family and then rejects the request. A normal rollback would undo the revocation,
     * leaving the stolen tokens live. The only write on any throwing path is that revocation.
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthResponse refresh(String rawToken, HttpServletRequest httpRequest) {
        String hash = jwtService.hashRefreshToken(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (!stored.isActive()) {
            log.warn("Reuse of an inactive refresh token for user {}; revoking all sessions",
                    stored.getUser().getUsername());
            refreshTokenRepository.revokeAllForUser(stored.getUser().getId(), Instant.now());
            throw new BadCredentialsException("Invalid refresh token");
        }

        User user = stored.getUser();
        if (!user.isActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        IssuedRefreshToken replacement = persistRefreshToken(user, httpRequest);
        stored.replaceWith(replacement.entity());

        UserPrincipal principal = UserPrincipal.from(user);
        return AuthResponse.of(jwtService.generateAccessToken(principal),
                replacement.rawValue(),
                jwtService.accessTokenTtl().toSeconds(),
                userMapper.toResponse(user));
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(jwtService.hashRefreshToken(rawToken))
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void logoutEverywhere(java.util.UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    private AuthResponse issueTokens(UserPrincipal principal, User user, HttpServletRequest httpRequest) {
        IssuedRefreshToken refreshToken = persistRefreshToken(user, httpRequest);
        return AuthResponse.of(jwtService.generateAccessToken(principal),
                refreshToken.rawValue(),
                jwtService.accessTokenTtl().toSeconds(),
                userMapper.toResponse(user));
    }

    /** The raw value is returned alongside the entity because only its hash is persisted. */
    private IssuedRefreshToken persistRefreshToken(User user, HttpServletRequest httpRequest) {
        String raw = jwtService.generateRefreshToken();
        RefreshToken token = new RefreshToken(user, jwtService.hashRefreshToken(raw),
                Instant.now().plus(jwtService.refreshTokenTtl()),
                httpRequest == null ? null : httpRequest.getHeader("User-Agent"),
                httpRequest == null ? null : httpRequest.getRemoteAddr());
        refreshTokenRepository.save(token);
        return new IssuedRefreshToken(token, raw);
    }

    private record IssuedRefreshToken(RefreshToken entity, String rawValue) {
    }
}
