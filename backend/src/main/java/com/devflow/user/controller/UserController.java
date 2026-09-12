package com.devflow.user.controller;

import com.devflow.common.PageResponse;
import com.devflow.common.SortProperties;
import com.devflow.security.UserPrincipal;
import com.devflow.user.dto.ChangePasswordRequest;
import com.devflow.user.dto.UpdateProfileRequest;
import com.devflow.user.dto.UpdateUserRequest;
import com.devflow.user.dto.UserResponse;
import com.devflow.user.entity.Role;
import com.devflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Account directory and administration")
public class UserController {

    private static final Set<String> SORTABLE = Set.of("fullName", "username", "email", "role",
            "active", "createdAt", "lastLoginAt");

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "List accounts, filtered and paginated")
    public PageResponse<UserResponse> list(@RequestParam(required = false) String q,
                                           @RequestParam(required = false) Role role,
                                           @RequestParam(required = false) Boolean active,
                                           @PageableDefault(size = 20, sort = "fullName",
                                                   direction = Sort.Direction.ASC) Pageable pageable) {
        return userService.search(q, role, active, SortProperties.validate(pageable, SORTABLE));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a single account")
    public UserResponse get(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an account's name, role or active state")
    public UserResponse update(@PathVariable UUID id,
                               @Valid @RequestBody UpdateUserRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        return userService.update(id, request, principal.getId());
    }

    @PutMapping("/me")
    @Operation(summary = "Update the current user's own profile")
    public UserResponse updateOwnProfile(@Valid @RequestBody UpdateProfileRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return userService.updateProfile(principal.getId(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change the current user's password")
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        userService.changePassword(principal.getId(), request);
    }
}
