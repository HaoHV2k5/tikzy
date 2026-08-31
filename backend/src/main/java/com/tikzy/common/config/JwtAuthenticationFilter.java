package com.tikzy.common.config;

import com.tikzy.auth.service.AccessTokenRevocationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc Bearer token ở mỗi request, xác thực, kiểm tra trạng thái revoke và nạp
 * Authentication vào SecurityContext. Cache Redis giúp không query DB mỗi request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenRevocationService accessTokenRevocationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Claims claims = jwtTokenProvider.getClaims(token);
            if (!isRevoked(claims)) {
                String role = claims.get("role", String.class);
                List<SimpleGrantedAuthority> authorities = StringUtils.hasText(role)
                        ? List.of(new SimpleGrantedAuthority(role))
                        : List.of();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isRevoked(Claims claims) {
        try {
            return accessTokenRevocationService.isRevoked(claims);
        } catch (RuntimeException ex) {
            // Không fail-open khi Redis/DB không thể xác minh trạng thái token.
            log.error("Không thể kiểm tra trạng thái revoke của access token", ex);
            return true;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX))
                ? header.substring(BEARER_PREFIX.length())
                : null;
    }
}
