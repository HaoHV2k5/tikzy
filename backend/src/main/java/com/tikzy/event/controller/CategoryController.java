package com.tikzy.event.controller;

import com.tikzy.common.response.ApiResponse;
import com.tikzy.event.dto.request.CreateCategoryRequest;
import com.tikzy.event.dto.request.UpdateCategoryRequest;
import com.tikzy.event.dto.response.CategoryResponse;
import com.tikzy.event.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/categories")
    public ApiResponse<List<CategoryResponse>> getPublished(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách danh mục thành công",
                categoryService.getPublished(PageRequest.of(page, size, categorySort())));
    }

    @GetMapping("/categories/{categoryId}")
    public ApiResponse<CategoryResponse> getPublishedById(@PathVariable UUID categoryId) {
        return ApiResponse.ok(
                "Lấy thông tin danh mục thành công",
                categoryService.getPublishedById(categoryId));
    }

    @PostMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.ok(
                "Tạo danh mục thành công",
                categoryService.create(request));
    }

    @GetMapping("/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<CategoryResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách danh mục thành công",
                categoryService.getAll(status, PageRequest.of(page, size, categorySort())));
    }

    @GetMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> getById(@PathVariable UUID categoryId) {
        return ApiResponse.ok(
                "Lấy thông tin danh mục thành công",
                categoryService.getById(categoryId));
    }

    @PatchMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> update(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.ok(
                "Cập nhật danh mục thành công",
                categoryService.update(categoryId, request));
    }

    @PostMapping("/admin/categories/{categoryId}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> publish(@PathVariable UUID categoryId) {
        return ApiResponse.ok(
                "Công khai danh mục thành công",
                categoryService.publish(categoryId));
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> archive(@PathVariable UUID categoryId) {
        return ApiResponse.ok(
                "Lưu trữ danh mục thành công",
                categoryService.archive(categoryId));
    }

    private Sort categorySort() {
        return Sort.by(Sort.Direction.ASC, "sortOrder")
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
