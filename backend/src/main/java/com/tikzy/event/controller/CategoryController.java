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
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ApiResponse<CategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.ok(
                "Tạo danh mục thành công",
                categoryService.create(request));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "sortOrder")
                .and(Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.page(
                "Lấy danh sách danh mục thành công",
                categoryService.getAll(status, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getById(@PathVariable UUID categoryId) {
        return ApiResponse.ok(
                "Lấy thông tin danh mục thành công",
                categoryService.getById(categoryId));
    }

    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> update(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.ok(
                "Cập nhật danh mục thành công",
                categoryService.update(categoryId, request));
    }

    @PostMapping("/{categoryId}/publish")
    public ApiResponse<CategoryResponse> publish(@PathVariable UUID categoryId) {
        return ApiResponse.ok(
                "Công khai danh mục thành công",
                categoryService.publish(categoryId));
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> archive(@PathVariable UUID categoryId) {
        return ApiResponse.ok(
                "Lưu trữ danh mục thành công",
                categoryService.archive(categoryId));
    }
}
