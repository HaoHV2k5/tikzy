package com.tikzy.auth.dto.response;

public record AccountUnlockVerificationResponse(
        String resetToken,
        long expiresInSeconds) {
}
