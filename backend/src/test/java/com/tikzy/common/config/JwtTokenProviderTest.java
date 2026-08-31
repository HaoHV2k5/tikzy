package com.tikzy.common.config;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String SECRET =
            "test-secret-key-test-secret-key-test-secret-key-test-secret-key-0123456789";
    private static final long EXPIRATION_MS = 1_800_000L;

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, EXPIRATION_MS);

    private User buildUser() {
        Role role = Role.builder().code("ROLE_CUSTOMER").name("Khách hàng").build();
        User user = User.builder()
                .role(role)
                .email("test@example.com")
                .passwordHash("hash")
                .fullName("Test User")
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    @Test
    void generateToken_validateAndParseClaims() {
        String token = provider.generateAccessToken(buildUser());

        assertNotNull(token);
        assertTrue(provider.validateToken(token));

        Claims claims = provider.getClaims(token);
        assertNotNull(claims.getId());
        assertEquals("test@example.com", claims.getSubject());
        assertEquals("ROLE_CUSTOMER", claims.get("role", String.class));
        assertNotNull(claims.get("userId", String.class));
        assertEquals(0, claims.get("tokenVersion", Integer.class));
    }

    @Test
    void validateToken_garbageToken_returnsFalse() {
        assertFalse(provider.validateToken("not-a-jwt"));
    }

    @Test
    void validateToken_wrongSecret_returnsFalse() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(SECRET + "-different", EXPIRATION_MS);
        String token = otherProvider.generateAccessToken(buildUser());

        assertFalse(provider.validateToken(token));
    }

    @Test
    void validateToken_expired_returnsFalse() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000L);
        String token = expiredProvider.generateAccessToken(buildUser());

        assertFalse(provider.validateToken(token));
    }

    @Test
    void expirationSeconds_convertsFromMs() {
        assertEquals(1800L, provider.getAccessTokenExpirationSeconds());
    }
}
