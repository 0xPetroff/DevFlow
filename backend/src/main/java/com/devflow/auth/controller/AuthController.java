package com.devflow.auth.controller;

import com.devflow.auth.dto.AuthResponse;
import com.devflow.auth.dto.LoginRequest;
import com.devflow.auth.dto.RefreshRequest;
import com.devflow.auth.dto.RegisterRequest;
import com.devflow.auth.service.AuthService;
import com.devflow.security.UserPrincipal;
import com.devflow.user.dto.UserResponse;
import com.devflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login and token lifecycle")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account. The first account created becomes an ADMIN.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, httpRequest));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange credentials for an access token and a refresh token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token for a new access token")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpRequest) {
        return authService.refresh(request.refreshToken(), httpRequest);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the supplied refresh token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Revoke every refresh token belonging to the current user")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutEverywhere(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logoutEverywhere(principal.getId());
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user")
    public UserResponse currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getById(principal.getId());
    }
}
