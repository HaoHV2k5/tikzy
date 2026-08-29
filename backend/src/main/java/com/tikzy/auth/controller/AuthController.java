package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.LoginRequest;
import com.tikzy.auth.dto.request.RegisterRequest;
import com.tikzy.auth.dto.response.AuthResponse;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.service.AuthService;
import com.tikzy.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Đăng ký tài khoản thành công", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Đăng nhập thành công", authService.login(request));
    }
}
