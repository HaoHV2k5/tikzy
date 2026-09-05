package com.tikzy.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token khôi phục không được để trống")
    @Size(max = 128, message = "Token khôi phục không hợp lệ")
    private String resetToken;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
    private String newPassword;

    @NotBlank(message = "Mật khẩu xác nhận không được để trống")
    @Size(min = 8, max = 64, message = "Mật khẩu phải từ 8 đến 64 ký tự")
    private String confirmPassword;
}
