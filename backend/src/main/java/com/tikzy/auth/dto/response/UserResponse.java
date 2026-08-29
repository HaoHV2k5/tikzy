package com.tikzy.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID id;
    private String email;
    private String phone;
    private String fullName;
    private String avatarUrl;
    private String role;
    private LocalDateTime createdAt;
}
