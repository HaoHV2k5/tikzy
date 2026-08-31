package com.tikzy.auth.service;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessTokenRevocationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String VERSION_KEY = "auth:user:token-version:" + USER_ID;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private UserRepository userRepository;

    private AccessTokenRevocationService revocationService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        revocationService = new AccessTokenRevocationService(redisTemplate, userRepository);
    }

    @Test
    void blacklist_storesJtiUntilAccessTokenExpires() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getId()).thenReturn("jti-1");
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() + 60_000));

        revocationService.blacklist(claims);

        verify(valueOperations).set(
                eq("auth:access:blacklist:jti-1"),
                eq("1"),
                longThat(ttl -> ttl > 0 && ttl <= 60_000),
                eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void isRevoked_blacklistedJti_returnsTrue() {
        Claims claims = claims(1);
        when(redisTemplate.hasKey("auth:access:blacklist:jti-1")).thenReturn(true);

        assertTrue(revocationService.isRevoked(claims));
    }

    @Test
    void isRevoked_staleTokenVersion_returnsTrue() {
        Claims claims = claims(0);
        when(redisTemplate.hasKey("auth:access:blacklist:jti-1")).thenReturn(false);
        when(valueOperations.get(VERSION_KEY)).thenReturn("1");

        assertTrue(revocationService.isRevoked(claims));
    }

    @Test
    void isRevoked_matchingTokenVersion_returnsFalse() {
        Claims claims = claims(1);
        when(redisTemplate.hasKey("auth:access:blacklist:jti-1")).thenReturn(false);
        when(valueOperations.get(VERSION_KEY)).thenReturn("1");

        assertFalse(revocationService.isRevoked(claims));
    }

    @Test
    void isRevoked_cacheMiss_loadsVersionFromDatabase() {
        Claims claims = claims(1);
        when(redisTemplate.hasKey("auth:access:blacklist:jti-1")).thenReturn(false);
        when(valueOperations.get(VERSION_KEY)).thenReturn(null);
        when(userRepository.findTokenVersionById(USER_ID)).thenReturn(Optional.of(1));

        assertFalse(revocationService.isRevoked(claims));
        verify(valueOperations).set(VERSION_KEY, "1");
    }

    @Test
    void invalidateAll_incrementsAndCachesUserVersion() {
        User user = User.builder()
                .role(Role.builder().code("ROLE_CUSTOMER").name("Khách hàng").build())
                .email("user@example.com")
                .passwordHash("hash")
                .fullName("User")
                .tokenVersion(2)
                .build();
        user.setId(USER_ID);

        revocationService.invalidateAll(user);

        assertEquals(3, user.getTokenVersion());
        verify(valueOperations).set(VERSION_KEY, "3");
    }

    private Claims claims(int tokenVersion) {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getId()).thenReturn("jti-1");
        when(claims.get("userId", String.class)).thenReturn(USER_ID.toString());
        when(claims.get("tokenVersion")).thenReturn(tokenVersion);
        return claims;
    }
}
