package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.UpdateProfileRequest;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.service.UserService;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile(Authentication authentication) {
        return ApiResponse.ok(
                "Lấy thông tin cá nhân thành công",
                userService.getMyProfile(currentUserEmail(authentication)));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(
                "Cập nhật thông tin cá nhân thành công",
                userService.updateMyProfile(currentUserEmail(authentication), request));
    }

    private String currentUserEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
