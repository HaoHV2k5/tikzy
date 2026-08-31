package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.UpdateProfileRequest;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.mapper.UserMapper;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
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

    private User findUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeNullable(String value) {
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
