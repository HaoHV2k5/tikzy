package com.tikzy.auth.service;

import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Lưu trạng thái thu hồi access token bên ngoài JWT stateless.
 * JTI dùng cho logout một thiết bị, token version dùng cho logout toàn bộ.
 */
@Service
@RequiredArgsConstructor
public class AccessTokenRevocationService {

    private static final String ACCESS_TOKEN_BLACKLIST_PREFIX = "auth:access:blacklist:";
    private static final String USER_TOKEN_VERSION_PREFIX = "auth:user:token-version:";
    private static final String BLACKLISTED_VALUE = "1";

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    /**
     * Blacklist JTI cho tới khi access token tự hết hạn.
     */
    public void blacklist(Claims claims) {
        String jti = claims.getId();
        if (!StringUtils.hasText(jti) || claims.getExpiration() == null) {
            return;
        }

        long ttlMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttlMillis > 0) {
            redisTemplate.opsForValue().set(
                    accessTokenBlacklistKey(jti),
                    BLACKLISTED_VALUE,
                    ttlMillis,
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Tăng version bền vững trong entity và đồng bộ cache Redis.
     */
    public void invalidateAll(User user) {
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        int nextVersion = currentVersion + 1;
        user.setTokenVersion(nextVersion);

        if (user.getId() != null) {
            redisTemplate.opsForValue().set(
                    userTokenVersionKey(user.getId()),
                    Integer.toString(nextVersion));
        }
    }

    /**
     * Kiểm tra blacklist JTI và version user sau khi JWT đã được xác thực chữ ký.
     * Cache miss chỉ query DB một lần rồi ghi lại vào Redis.
     */
    public boolean isRevoked(Claims claims) {
        String jti = claims.getId();
        String userIdValue = claims.get("userId", String.class);
        Object tokenVersionValue = claims.get("tokenVersion");

        if (!StringUtils.hasText(jti)
                || !StringUtils.hasText(userIdValue)
                || !(tokenVersionValue instanceof Number)) {
            return true;
        }

        if (Boolean.TRUE.equals(redisTemplate.hasKey(accessTokenBlacklistKey(jti)))) {
            return true;
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdValue);
        } catch (IllegalArgumentException ex) {
            return true;
        }

        Integer currentVersion = resolveCurrentTokenVersion(userId);
        return currentVersion == null
                || ((Number) tokenVersionValue).intValue() != currentVersion;
    }

    private Integer resolveCurrentTokenVersion(UUID userId) {
        String key = userTokenVersionKey(userId);
        String cachedVersion = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cachedVersion)) {
            try {
                return Integer.valueOf(cachedVersion);
            } catch (NumberFormatException ignored) {
                // Fall through to the persisted source of truth.
            }
        }

        Integer persistedVersion = userRepository.findTokenVersionById(userId).orElse(null);
        if (persistedVersion != null) {
            redisTemplate.opsForValue().set(key, Integer.toString(persistedVersion));
        }
        return persistedVersion;
    }

    private String accessTokenBlacklistKey(String jti) {
        return ACCESS_TOKEN_BLACKLIST_PREFIX + jti;
    }

    private String userTokenVersionKey(UUID userId) {
        return USER_TOKEN_VERSION_PREFIX + userId;
    }
}
