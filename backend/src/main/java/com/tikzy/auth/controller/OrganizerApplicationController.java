package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.CreateOrganizerApplicationRequest;
import com.tikzy.auth.dto.request.OrganizerApplicationPageRequest;
import com.tikzy.auth.dto.request.ReviewOrganizerApplicationRequest;
import com.tikzy.auth.dto.response.OrganizerApplicationResponse;
import com.tikzy.auth.service.OrganizerApplicationService;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrganizerApplicationController {

    private final OrganizerApplicationService organizerApplicationService;

    @PostMapping("/organizer-applications")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrganizerApplicationResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateOrganizerApplicationRequest request) {
        return ApiResponse.ok(
                "Gửi hồ sơ đăng ký ban tổ chức thành công",
                organizerApplicationService.create(currentUserEmail(authentication), request));
    }

    @GetMapping("/organizer-applications/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ORGANIZER')")
    public ApiResponse<OrganizerApplicationResponse> getMyLatestApplication(
            Authentication authentication) {
        return ApiResponse.ok(
                "Lấy hồ sơ đăng ký ban tổ chức thành công",
                organizerApplicationService.getLatestForApplicant(
                        currentUserEmail(authentication)));
    }

    @GetMapping("/admin/organizer-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OrganizerApplicationResponse>> getApplications(
            @RequestParam(required = false) String status,
            @Parameter(description = "Số trang, bắt đầu từ 0", example = "0")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Số phần tử mỗi trang", example = "10")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Sắp xếp theo dạng property,direction", example = "createdAt,desc")
            @RequestParam(required = false) String sort) {
        OrganizerApplicationPageRequest pageRequest = new OrganizerApplicationPageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        pageRequest.setSort(sort == null ? null : List.of(sort));
        return ApiResponse.page(
                "Lấy danh sách hồ sơ đăng ký ban tổ chức thành công",
                organizerApplicationService.getApplications(
                        status,
                        pageRequest.toPageable()));
    }

    @PatchMapping("/admin/organizer-applications/{applicationId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrganizerApplicationResponse> review(
            @PathVariable UUID applicationId,
            Authentication authentication,
            @Valid @RequestBody ReviewOrganizerApplicationRequest request) {
        return ApiResponse.ok(
                "Xử lý hồ sơ đăng ký ban tổ chức thành công",
                organizerApplicationService.review(
                        applicationId,
                        currentUserEmail(authentication),
                        request));
    }

    private String currentUserEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
