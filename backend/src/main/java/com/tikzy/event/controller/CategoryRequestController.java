package com.tikzy.event.controller;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.response.ApiResponse;
import com.tikzy.event.dto.request.CreateCategoryProposalRequest;
import com.tikzy.event.dto.request.ReviewCategoryRequest;
import com.tikzy.event.dto.response.CategoryRequestResponse;
import com.tikzy.event.service.CategoryRequestService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class CategoryRequestController {

    private final CategoryRequestService categoryRequestService;

    @PostMapping("/category-requests")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ApiResponse<CategoryRequestResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateCategoryProposalRequest request) {
        return ApiResponse.ok(
                "Gửi đề xuất danh mục thành công",
                categoryRequestService.create(currentUserEmail(authentication), request));
    }

    @GetMapping("/admin/category-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CategoryRequestResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách đề xuất danh mục thành công",
                categoryRequestService.getAll(
                        status,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @PatchMapping("/admin/category-requests/{requestId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryRequestResponse> review(
            @PathVariable UUID requestId,
            Authentication authentication,
            @Valid @RequestBody ReviewCategoryRequest request) {
        return ApiResponse.ok(
                "Xử lý đề xuất danh mục thành công",
                categoryRequestService.review(
                        requestId,
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
