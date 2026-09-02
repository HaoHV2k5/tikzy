package com.tikzy.auth.dto.response;

import java.time.LocalDateTime;

public record SecurityPolicyResponse(
        Integer maxFailedLoginAttempts,
        LocalDateTime updatedAt) {
}
