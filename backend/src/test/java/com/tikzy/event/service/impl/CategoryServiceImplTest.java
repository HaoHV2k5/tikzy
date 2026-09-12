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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    private final CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);
    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryRepository, categoryMapper);
    }

    @Test
    void create_withDefaults_savesNormalizedCategory() {
        CreateCategoryRequest request = request();
        request.setName("  Triển lãm  ");
        request.setIconUrl("   ");
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category category = invocation.getArgument(0);
                    category.setId(UUID.randomUUID());
                    category.setCreatedAt(LocalDateTime.now());
                    return category;
                });

        CategoryResponse response = categoryService.create(request);

        assertEquals("Triển lãm", response.getName());
        assertEquals("trien-lam", response.getSlug());
        assertEquals(0, response.getSortOrder());
        assertEquals(CategoryStatus.DRAFT, response.getStatus());
        assertNull(response.getIconUrl());
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).saveAndFlush(captor.capture());
        assertEquals("triển lãm", captor.getValue().getNormalizedName());
    }

    @Test
    void create_whenNormalizedNameExists_throwsConflict() {
        CreateCategoryRequest request = request();
        when(categoryRepository.existsByNormalizedNameAndStatusIn(
                eq("triển lãm"),
                any())).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> categoryService.create(request));

        assertEquals(ErrorCode.CATEGORY_ALREADY_EXISTS, exception.getErrorCode());
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void create_whenSlugExists_throwsConflict() {
        CreateCategoryRequest request = request();
        when(categoryRepository.existsBySlug("trien-lam")).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> categoryService.create(request));

        assertEquals(ErrorCode.CATEGORY_ALREADY_EXISTS, exception.getErrorCode());
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void create_whenConcurrentInsertUsesSlug_throwsConflict() {
        CreateCategoryRequest request = request();
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate slug"));

        AppException exception = assertThrows(
                AppException.class,
                () -> categoryService.create(request));

        assertEquals(ErrorCode.CATEGORY_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void publish_draftCategory_setsPublishedStatus() {
        UUID categoryId = UUID.randomUUID();
        Category category = category();
        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.publish(categoryId);

        assertEquals(CategoryStatus.PUBLISHED, response.getStatus());
        verify(categoryRepository).save(category);
    }

    @Test
    void publish_publishedCategory_throwsInvalidStatus() {
        UUID categoryId = UUID.randomUUID();
        Category category = category();
        category.setStatus(CategoryStatus.PUBLISHED);
        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));

        AppException exception = assertThrows(
                AppException.class,
                () -> categoryService.publish(categoryId));

        assertEquals(ErrorCode.INVALID_CATEGORY_STATUS, exception.getErrorCode());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_publishedCategoryCannotChangeSlug() {
        UUID categoryId = UUID.randomUUID();
        Category category = category();
        category.setStatus(CategoryStatus.PUBLISHED);
        UpdateCategoryRequest request = new UpdateCategoryRequest();
        request.setSlug("slug-moi");
        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));

        AppException exception = assertThrows(
                AppException.class,
                () -> categoryService.update(categoryId, request));

        assertEquals(ErrorCode.INVALID_CATEGORY_STATUS, exception.getErrorCode());
        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void archive_publishedCategory_setsArchivedStatus() {
        UUID categoryId = UUID.randomUUID();
        Category category = category();
        category.setStatus(CategoryStatus.PUBLISHED);
        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.archive(categoryId);

        assertEquals(CategoryStatus.ARCHIVED, response.getStatus());
        verify(categoryRepository).save(category);
    }

    private CreateCategoryRequest request() {
        CreateCategoryRequest request = new CreateCategoryRequest();
        request.setName("Triển lãm");
        request.setSlug("trien-lam");
        return request;
    }

    private Category category() {
        Category category = Category.builder()
                .name("Triển lãm")
                .slug("trien-lam")
                .sortOrder(0)
                .status(CategoryStatus.DRAFT)
                .build();
        category.setId(UUID.randomUUID());
        return category;
    }

}
