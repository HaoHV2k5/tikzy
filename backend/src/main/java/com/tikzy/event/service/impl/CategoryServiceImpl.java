package com.tikzy.event.service.impl;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.dto.request.CreateCategoryRequest;
import com.tikzy.event.dto.request.UpdateCategoryRequest;
import com.tikzy.event.dto.response.CategoryResponse;
import com.tikzy.event.entity.Category;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.mapper.CategoryMapper;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.service.CategoryService;
import com.tikzy.event.util.CategoryNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Set<CategoryStatus> ACTIVE_STATUSES =
            Set.of(CategoryStatus.DRAFT, CategoryStatus.PUBLISHED);

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String slug = request.getSlug().trim();
        String normalizedName = CategoryNameNormalizer.normalize(request.getName());
        categoryRepository.lockNormalizedName(normalizedName);
        if (categoryRepository.existsBySlug(slug)
                || categoryRepository.existsByNormalizedNameAndStatusIn(
                        normalizedName,
                        ACTIVE_STATUSES)) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = categoryMapper.toEntity(request);
        category.setName(CategoryNameNormalizer.clean(request.getName()));
        category.setNormalizedName(normalizedName);
        category.setSlug(slug);
        category.setIconUrl(normalizeNullable(request.getIconUrl()));
        category.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        category.setStatus(CategoryStatus.DRAFT);
        return save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAll(String status, Pageable pageable) {
        Page<Category> categories = StringUtils.hasText(status)
                ? categoryRepository.findAllByStatus(parseStatus(status), pageable)
                : categoryRepository.findAll(pageable);
        return categories.map(categoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(UUID categoryId) {
        return categoryMapper.toResponse(findCategory(categoryId));
    }

    @Override
    @Transactional
    public CategoryResponse update(UUID categoryId, UpdateCategoryRequest request) {
        Category category = findCategory(categoryId);
        if (category.getStatus() == CategoryStatus.ARCHIVED) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_STATUS);
        }

        String requestedSlug = request.getSlug();
        if (requestedSlug != null) {
            requestedSlug = requestedSlug.trim();
            if (category.getStatus() == CategoryStatus.PUBLISHED
                    && !requestedSlug.equals(category.getSlug())) {
                throw new AppException(
                        ErrorCode.INVALID_CATEGORY_STATUS,
                        "Không thể đổi slug của danh mục đã được công khai");
            }
            if (!requestedSlug.equals(category.getSlug())
                    && categoryRepository.existsBySlugAndIdNot(requestedSlug, categoryId)) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            request.setSlug(requestedSlug);
        }
        if (request.getName() != null && !StringUtils.hasText(request.getName())) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_DATA);
        }
        String normalizedName = null;
        if (request.getName() != null) {
            normalizedName = CategoryNameNormalizer.normalize(request.getName());
            categoryRepository.lockNormalizedName(normalizedName);
            if (!normalizedName.equals(category.getNormalizedName())
                    && categoryRepository.existsByNormalizedNameAndStatusInAndIdNot(
                            normalizedName,
                            ACTIVE_STATUSES,
                            categoryId)) {
                throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
        }

        categoryMapper.updateEntity(request, category);
        if (request.getName() != null) {
            category.setName(CategoryNameNormalizer.clean(request.getName()));
            category.setNormalizedName(normalizedName);
        }
        if (request.getIconUrl() != null) {
            category.setIconUrl(normalizeNullable(request.getIconUrl()));
        }
        return save(category);
    }

    @Override
    @Transactional
    public CategoryResponse publish(UUID categoryId) {
        Category category = findCategory(categoryId);
        if (category.getStatus() != CategoryStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_STATUS);
        }
        category.setStatus(CategoryStatus.PUBLISHED);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse archive(UUID categoryId) {
        Category category = findCategory(categoryId);
        if (category.getStatus() != CategoryStatus.ARCHIVED) {
            category.setStatus(CategoryStatus.ARCHIVED);
            category = categoryRepository.save(category);
        }
        return categoryMapper.toResponse(category);
    }

    private CategoryResponse save(Category category) {
        try {
            return categoryMapper.toResponse(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    private Category findCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private CategoryStatus parseStatus(String status) {
        try {
            return CategoryStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException(
                    ErrorCode.INVALID_CATEGORY_DATA,
                    "Trạng thái danh mục không hợp lệ");
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
