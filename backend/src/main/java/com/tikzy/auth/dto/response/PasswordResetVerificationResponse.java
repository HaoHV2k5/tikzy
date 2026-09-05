package com.tikzy.auth.dto.response;

public record PasswordResetVerificationResponse(
        String resetToken,
        long expiresInSeconds) {
}
