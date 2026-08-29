package com.tikzy.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private String refreshToken;

    @JsonIgnore
    public String getRefreshToken() {
        return refreshToken;
    }
}
