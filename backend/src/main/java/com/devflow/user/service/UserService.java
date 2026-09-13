package com.devflow.user.service;

import com.devflow.audit.entity.AuditAction;
import com.devflow.audit.service.AuditService;
import com.devflow.common.PageResponse;
import com.devflow.exception.BusinessRuleException;
import com.devflow.exception.ResourceNotFoundException;
import com.devflow.user.dto.ChangePasswordRequest;
import com.devflow.user.dto.UpdateProfileRequest;
import com.devflow.user.dto.UpdateUserRequest;
import com.devflow.user.dto.UserResponse;
import com.devflow.user.entity.Role;
import com.devflow.user.entity.User;
import com.devflow.user.mapper.UserMapper;
import com.devflow.user.repository.UserRepository;
import com.devflow.user.repository.UserSpecifications;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       PasswordEncoder passwordEncoder, AuditService auditService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public UserResponse getById(UUID id) {
        return userMapper.toResponse(requireUser(id));
    }

    public PageResponse<UserResponse> search(String query, Role role, Boolean active, Pageable pageable) {
        return PageResponse.from(userRepository
                .findAll(UserSpecifications.matching(query, role, active), pageable)
                .map(userMapper::toResponse));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = requireUser(id);
        user.setFullName(request.fullName().trim());
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request, UUID actingAdminId) {
        User user = requireUser(id);

        // Removing your own admin rights could leave the instance with no administrator.
        boolean demotingSelf = user.getId().equals(actingAdminId) && request.role() != Role.ADMIN;
        boolean disablingSelf = user.getId().equals(actingAdminId) && !request.active();
        if (demotingSelf || disablingSelf) {
            throw new BusinessRuleException("You cannot remove your own administrator access");
        }

        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setActive(request.active());

        auditService.record(AuditAction.USER_UPDATED, "User", user.getId(), null,
                "Updated account %s".formatted(user.getUsername()),
                Map.of("role", request.role().name(), "active", request.active()));
        return userMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = requireUser(id);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    public User requireUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }
}
