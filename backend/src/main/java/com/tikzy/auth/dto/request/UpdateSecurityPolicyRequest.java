package com.tikzy.auth.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSecurityPolicyRequest {

    @NotNull(message = "Số lần đăng nhập sai tối đa không được để trống")
    @Min(value = 1, message = "Số lần đăng nhập sai tối đa phải lớn hơn 0")
    @Max(value = 100, message = "Số lần đăng nhập sai tối đa không được vượt quá 100")
    private Integer maxFailedLoginAttempts;
}
