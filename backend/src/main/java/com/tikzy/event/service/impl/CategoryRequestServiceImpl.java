package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.dto.request.CreateCategoryProposalRequest;
import com.tikzy.event.dto.request.CreateCategoryRequest;
import com.tikzy.event.dto.request.ReviewCategoryRequest;
import com.tikzy.event.dto.response.CategoryRequestResponse;
import com.tikzy.event.dto.response.CategoryResponse;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.CategoryRequest;
import com.tikzy.event.enums.CategoryRequestStatus;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.mapper.CategoryRequestMapper;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.CategoryRequestRepository;
import com.tikzy.event.service.CategoryRequestService;
import com.tikzy.event.service.CategoryService;
import com.tikzy.event.util.CategoryNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryRequestServiceImpl implements CategoryRequestService {

    private static final Set<CategoryStatus> ACTIVE_CATEGORY_STATUSES =
            Set.of(CategoryStatus.DRAFT, CategoryStatus.PUBLISHED);

    private final CategoryRequestRepository categoryRequestRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final CategoryRequestMapper categoryRequestMapper;

    @Override
    @Transactional
    public CategoryRequestResponse create(
            String requesterEmail,
            CreateCategoryProposalRequest request) {
        User requester = findUser(requesterEmail);
        String proposedName = CategoryNameNormalizer.clean(request.getProposedName());

        CategoryRequest categoryRequest = categoryRequestMapper.toEntity(request);
        categoryRequest.setRequester(requester);
        categoryRequest.setProposedName(proposedName);
        categoryRequest.setNormalizedProposedName(
                CategoryNameNormalizer.normalize(proposedName));
        categoryRequest.setDescription(normalizeNullable(request.getDescription()));
        categoryRequest.setStatus(CategoryRequestStatus.PENDING);
        return categoryRequestMapper.toResponse(
                categoryRequestRepository.saveAndFlush(categoryRequest));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryRequestResponse> getAll(String status, Pageable pageable) {
        Page<CategoryRequest> requests = StringUtils.hasText(status)
                ? categoryRequestRepository.findAllByStatus(parseStatus(status), pageable)
                : categoryRequestRepository.findAll(pageable);
        return requests.map(categoryRequestMapper::toResponse);
    }

    @Override
    @Transactional
    public CategoryRequestResponse review(
            UUID requestId,
            String reviewerEmail,
            ReviewCategoryRequest reviewRequest) {
        CategoryRequest categoryRequest = categoryRequestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_REQUEST_NOT_FOUND));
        if (categoryRequest.getStatus() != CategoryRequestStatus.PENDING) {
            throw new AppException(ErrorCode.CATEGORY_REQUEST_ALREADY_REVIEWED);
        }

        CategoryRequestStatus result = parseReviewStatus(reviewRequest.getStatus());
        String reviewNote = normalizeNullable(reviewRequest.getReviewNote());
        if (result == CategoryRequestStatus.REJECTED && reviewNote == null) {
            throw new AppException(
                    ErrorCode.INVALID_CATEGORY_REQUEST,
                    "Từ chối đề xuất phải có lý do");
        }

        if (result == CategoryRequestStatus.APPROVED) {
            categoryRequest.setCategory(findOrCreateCategory(categoryRequest, reviewRequest));
        }
        categoryRequest.setStatus(result);
        categoryRequest.setReviewNote(reviewNote);
        categoryRequest.setReviewedBy(findUser(reviewerEmail));
        categoryRequest.setReviewedAt(LocalDateTime.now());
        return categoryRequestMapper.toResponse(
                categoryRequestRepository.save(categoryRequest));
    }

    private Category findOrCreateCategory(
            CategoryRequest categoryRequest,
            ReviewCategoryRequest reviewRequest) {
        String normalizedName = categoryRequest.getNormalizedProposedName();
        categoryRepository.lockNormalizedName(normalizedName);
        Optional<Category> existingCategory =
                categoryRepository.findFirstByNormalizedNameAndStatusIn(
                        normalizedName,
                        ACTIVE_CATEGORY_STATUSES);
        if (existingCategory.isPresent()) {
            return existingCategory.get();
        }

        if (!StringUtils.hasText(reviewRequest.getSlug())) {
            throw new AppException(
                    ErrorCode.INVALID_CATEGORY_REQUEST,
                    "Duyệt đề xuất phải cung cấp slug danh mục");
        }

        CreateCategoryRequest createRequest = new CreateCategoryRequest();
        createRequest.setName(categoryRequest.getProposedName());
        createRequest.setSlug(reviewRequest.getSlug());
        createRequest.setIconUrl(reviewRequest.getIconUrl());
        createRequest.setSortOrder(reviewRequest.getSortOrder());
        CategoryResponse category = categoryService.create(createRequest);
        return categoryRepository.findById(category.getId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private User findUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private CategoryRequestStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new AppException(
                    ErrorCode.INVALID_CATEGORY_REQUEST,
                    "Trạng thái đề xuất danh mục không hợp lệ");
        }
        try {
            return CategoryRequestStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException(
                    ErrorCode.INVALID_CATEGORY_REQUEST,
                    "Trạng thái đề xuất danh mục không hợp lệ");
        }
    }

    private CategoryRequestStatus parseReviewStatus(String status) {
        CategoryRequestStatus result = parseStatus(status);
        if (result == CategoryRequestStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_REQUEST);
        }
        return result;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
