package com.tikzy.auth.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateUserRequest {

    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    @Pattern(regexp = ".*\\S.*", message = "Họ tên không được để trống")
    private String fullName;

    @Pattern(regexp = "^(0\\d{9,10})?$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 500, message = "Ảnh đại diện tối đa 500 ký tự")
    private String avatarUrl;

    @Pattern(
            regexp = "ROLE_(CUSTOMER|ORGANIZER|ADMIN)",
            message = "Role không hợp lệ")
    private String role;

    private Boolean isActive;
}
