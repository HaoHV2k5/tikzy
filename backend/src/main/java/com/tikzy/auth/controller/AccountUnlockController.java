package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.ResetAccountPasswordRequest;
import com.tikzy.auth.dto.request.UnlockAccountRequest;
import com.tikzy.auth.dto.request.VerifyAccountUnlockOtpRequest;
import com.tikzy.auth.dto.response.AccountUnlockVerificationResponse;
import com.tikzy.auth.service.AccountUnlockService;
import com.tikzy.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/account-unlock")
@RequiredArgsConstructor
public class AccountUnlockController {

    private final AccountUnlockService accountUnlockService;

    @PostMapping("/request")
    public ApiResponse<Void> requestAccountUnlock(@Valid @RequestBody UnlockAccountRequest request) {
        accountUnlockService.requestUnlock(request);
        return ApiResponse.ok(
                "Nếu email thuộc tài khoản đang bị khóa, hệ thống đã gửi mã OTP đến email đó",
                null);
    }

    @PostMapping("/verify-otp")
    public ApiResponse<AccountUnlockVerificationResponse> verifyAccountUnlockOtp(
            @Valid @RequestBody VerifyAccountUnlockOtpRequest request) {
        return ApiResponse.ok(
                "Xác minh OTP thành công, hãy đặt mật khẩu mới",
                accountUnlockService.verifyOtp(request));
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetAccountPassword(
            @Valid @RequestBody ResetAccountPasswordRequest request) {
        accountUnlockService.resetPassword(request);
        return ApiResponse.ok("Mở khóa tài khoản và đặt lại mật khẩu thành công", null);
    }
}
