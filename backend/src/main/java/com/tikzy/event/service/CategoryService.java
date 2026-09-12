package com.tikzy.event.service;

import com.tikzy.event.dto.request.CreateCategoryRequest;
import com.tikzy.event.dto.request.UpdateCategoryRequest;
import com.tikzy.event.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryService {

    CategoryResponse create(CreateCategoryRequest request);

    Page<CategoryResponse> getAll(String status, Pageable pageable);

    CategoryResponse getById(UUID categoryId);

    CategoryResponse update(UUID categoryId, UpdateCategoryRequest request);

    CategoryResponse publish(UUID categoryId);

    CategoryResponse archive(UUID categoryId);
}
