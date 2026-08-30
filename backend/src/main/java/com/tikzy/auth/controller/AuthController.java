package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.LoginRequest;
import com.tikzy.auth.dto.request.RegisterRequest;
import com.tikzy.auth.dto.response.AuthResponse;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.service.AuthService;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";

    private final AuthService authService;

    @Value("${jwt.refresh-token-cookie-name:refresh_token}")
    private String refreshTokenCookieName;

    @Value("${jwt.refresh-token-cookie-secure:false}")
    private boolean refreshTokenCookieSecure;

    @Value("${jwt.refresh-token-cookie-same-site:Lax}")
    private String refreshTokenCookieSameSite;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Đăng ký tài khoản thành công", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(
                loginRequest,
                request.getHeader(HttpHeaders.USER_AGENT),
                resolveClientIp(request));
        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ApiResponse.ok("Đăng nhập thành công", authResponse);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.refresh(
                    resolveRefreshToken(request),
                    request.getHeader(HttpHeaders.USER_AGENT),
                    resolveClientIp(request));
            setRefreshTokenCookie(response, authResponse.getRefreshToken());
            return ApiResponse.ok("Làm mới token thành công", authResponse);
        } catch (AppException ex) {
            clearRefreshTokenCookie(response);
            throw ex;
        }
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(
                refreshToken, Duration.ofMillis(refreshTokenExpirationMs)).toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshTokenCookie(
                "", Duration.ZERO).toString());
    }

    private ResponseCookie buildRefreshTokenCookie(String value, Duration maxAge) {
        return ResponseCookie.from(refreshTokenCookieName, value)
                .httpOnly(true)
                .secure(refreshTokenCookieSecure)
                .sameSite(refreshTokenCookieSameSite)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (refreshTokenCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",", 2)[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank() ? realIp.trim() : request.getRemoteAddr();
    }
}
