package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.AdminUpdateUserRequest;
import com.tikzy.auth.dto.request.UpdateProfileRequest;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.mapper.UserMapper;
import com.tikzy.auth.repository.RefreshTokenRepository;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        return userMapper.toUserResponse(findUser(email));
    }

    @Transactional
    public UserResponse updateMyProfile(String email, UpdateProfileRequest request) {
        User user = findUser(email);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhone() != null) {
            String phone = normalizeNullable(request.getPhone());
            if (phone != null && userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(phone);
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserByAdmin(UUID userId, AdminUpdateUserRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        boolean securityStateChanged = false;

        if (request.getFullName() != null) {
            user.setFullName(normalizeRequiredText(request.getFullName()));
        }

        if (request.getPhone() != null) {
            String phone = normalizeNullable(request.getPhone());
            if (phone != null && userRepository.existsByPhoneAndIdNot(phone, user.getId())) {
                throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
            }
            user.setPhone(phone);
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));
        }

        if (request.getRole() != null) {
            String roleCode = request.getRole().trim().toUpperCase(Locale.ROOT);
            Role role = roleRepository.findByCode(roleCode)
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
            String currentRoleCode = user.getRole() == null ? null : user.getRole().getCode();
            if (!Objects.equals(currentRoleCode, role.getCode())) {
                user.setRole(role);
                securityStateChanged = true;
            }
        }

        if (request.getIsActive() != null
                && !Objects.equals(user.getIsActive(), request.getIsActive())) {
            user.setIsActive(request.getIsActive());
            securityStateChanged = true;
        }

        if (securityStateChanged) {
            accessTokenRevocationService.invalidateAll(user);
            refreshTokenRepository.revokeAllActiveByUser(user);
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    private User findUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeRequiredText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AppException(ErrorCode.INVALID_USER_DATA);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
