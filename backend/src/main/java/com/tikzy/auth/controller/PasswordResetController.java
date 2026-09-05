package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.ForgotPasswordRequest;
import com.tikzy.auth.dto.request.ResetPasswordRequest;
import com.tikzy.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.tikzy.auth.dto.response.PasswordResetVerificationResponse;
import com.tikzy.auth.service.PasswordResetService;
import com.tikzy.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ApiResponse<Void> requestPasswordReset(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return ApiResponse.ok(
                "Nếu email thuộc tài khoản hợp lệ, hệ thống đã gửi mã OTP đến email đó",
                null);
    }

    @PostMapping("/verify-otp")
    public ApiResponse<PasswordResetVerificationResponse> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request) {
        return ApiResponse.ok(
                "Xác minh OTP thành công, hãy đặt mật khẩu mới",
                passwordResetService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ApiResponse.ok("Đặt lại mật khẩu thành công", null);
    }
}
