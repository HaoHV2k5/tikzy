package com.tikzy.common.config;

import com.tikzy.auth.service.AccessTokenRevocationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AccessTokenRevocationService accessTokenRevocationService;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtTokenProvider, accessTokenRevocationService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void revokedAccessToken_isNotAuthenticated() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getClaims("access-token")).thenReturn(claims);
        when(accessTokenRevocationService.isRevoked(claims)).thenReturn(true);

        filter.doFilterInternal(request(), new MockHttpServletResponse(), filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void activeAccessToken_isAuthenticated() throws Exception {
        Claims claims = mock(Claims.class);
        when(jwtTokenProvider.validateToken("access-token")).thenReturn(true);
        when(jwtTokenProvider.getClaims("access-token")).thenReturn(claims);
        when(accessTokenRevocationService.isRevoked(claims)).thenReturn(false);
        when(claims.get("role", String.class)).thenReturn("ROLE_CUSTOMER");
        when(claims.getSubject()).thenReturn("user@example.com");

        filter.doFilterInternal(request(), new MockHttpServletResponse(), filterChain);

        assertEquals("user@example.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token");
        return request;
    }
}
