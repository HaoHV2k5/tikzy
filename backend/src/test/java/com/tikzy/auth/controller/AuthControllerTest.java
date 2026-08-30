package com.tikzy.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tikzy.auth.dto.request.LoginRequest;
import com.tikzy.auth.dto.response.AuthResponse;
import com.tikzy.auth.service.AuthService;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private static final long REFRESH_TOKEN_EXPIRATION_MS = 2_592_000_000L;

    private AuthService authService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        authController = new AuthController(authService);
        ReflectionTestUtils.setField(authController, "refreshTokenCookieName", "refresh_token");
        ReflectionTestUtils.setField(authController, "refreshTokenCookieSecure", true);
        ReflectionTestUtils.setField(authController, "refreshTokenCookieSameSite", "Lax");
        ReflectionTestUtils.setField(
                authController, "refreshTokenExpirationMs", REFRESH_TOKEN_EXPIRATION_MS);
    }

    @Test
    void login_setsRefreshTokenCookieAndHidesTokenFromJson() throws Exception {
        LoginRequest request = new LoginRequest();
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
        when(authService.login(request, "Mozilla/5.0", "203.0.113.42")).thenReturn(authResponse);
        HttpServletRequest httpRequest = requestWithHeaders();
        HttpServletResponse response = new MockHttpServletResponse();

        authController.login(request, httpRequest, response);

        verify(authService).login(request, "Mozilla/5.0", "203.0.113.42");

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refresh_token=refresh-token"));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("SameSite=Lax"));
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
        assertTrue(setCookie.contains("Max-Age=2592000"));

        String json = new ObjectMapper().writeValueAsString(authResponse);
        assertFalse(json.contains("refreshToken"));
        assertFalse(json.contains("refresh-token"));
    }

    @Test
    void refresh_rotatesCookieAndForwardsRequestMetadata() {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("rotated-access-token")
                .refreshToken("rotated-refresh-token")
                .build();
        MockHttpServletRequest request = requestWithHeaders();
        request.setCookies(new Cookie("refresh_token", "old-refresh-token"));
        when(authService.refresh("old-refresh-token", "Mozilla/5.0", "203.0.113.42"))
                .thenReturn(authResponse);
        MockHttpServletResponse response = new MockHttpServletResponse();

        authController.refresh(request, response);

        verify(authService).refresh("old-refresh-token", "Mozilla/5.0", "203.0.113.42");
        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refresh_token=rotated-refresh-token"));
        assertTrue(setCookie.contains("Max-Age=2592000"));
    }

    @Test
    void refresh_failure_clearsCookie() {
        MockHttpServletRequest request = requestWithHeaders();
        request.setCookies(new Cookie("refresh_token", "reused-refresh-token"));
        when(authService.refresh("reused-refresh-token", "Mozilla/5.0", "203.0.113.42"))
                .thenThrow(new AppException(ErrorCode.REFRESH_TOKEN_REUSED));
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(AppException.class, () -> authController.refresh(request, response));

        String clearCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(clearCookie);
        assertTrue(clearCookie.contains("refresh_token="));
        assertTrue(clearCookie.contains("Max-Age=0"));
    }

    private MockHttpServletRequest requestWithHeaders() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0");
        mockRequest.addHeader("X-Forwarded-For", "203.0.113.42, 10.0.0.1");
        return mockRequest;
    }
}
