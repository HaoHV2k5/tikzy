package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.UpdateSecurityPolicyRequest;
import com.tikzy.auth.dto.response.SecurityPolicyResponse;
import com.tikzy.auth.service.SecurityPolicyService;
import com.tikzy.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/security-policy")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    @GetMapping
    public ApiResponse<SecurityPolicyResponse> getPolicy() {
        return ApiResponse.ok(
                "Lấy chính sách bảo mật thành công",
                securityPolicyService.getPolicy());
    }

    @PatchMapping
    public ApiResponse<SecurityPolicyResponse> updatePolicy(
            @Valid @RequestBody UpdateSecurityPolicyRequest request) {
        return ApiResponse.ok(
                "Cập nhật số lần đăng nhập sai tối đa thành công",
                securityPolicyService.updatePolicy(request));
    }
}
