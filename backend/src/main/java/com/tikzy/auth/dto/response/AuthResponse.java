package com.tikzy.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Thời hạn access token, đơn vị giây */
    private long expiresIn;

    private UserResponse user;
}
