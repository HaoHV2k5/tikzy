package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.Role;
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
import com.tikzy.event.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryRequestServiceImplTest {

    @Mock
    private CategoryRequestRepository categoryRequestRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private CategoryRequestMapper categoryRequestMapper;

    private CategoryRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryRequestServiceImpl(
                categoryRequestRepository,
                categoryRepository,
                userRepository,
                categoryService,
                categoryRequestMapper);
    }

    @Test
    void create_normalizesAndSavesPendingRequest() {
        User requester = user("organizer@example.com", "ROLE_ORGANIZER");
        CreateCategoryProposalRequest request = proposal();
        request.setProposedName("  Nghệ thuật  ");
        CategoryRequest entity = CategoryRequest.builder().build();
        CategoryRequestResponse expected = CategoryRequestResponse.builder()
                .status(CategoryRequestStatus.PENDING)
                .build();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(requester));
        when(categoryRequestMapper.toEntity(request)).thenReturn(entity);
        when(categoryRequestRepository.saveAndFlush(entity)).thenReturn(entity);
        when(categoryRequestMapper.toResponse(entity)).thenReturn(expected);

        CategoryRequestResponse response = service.create(
                " Organizer@Example.com ",
                request);

        assertEquals(expected, response);
        assertEquals("Nghệ thuật", entity.getProposedName());
        assertEquals("nghệ thuật", entity.getNormalizedProposedName());
        assertEquals(requester, entity.getRequester());
        assertEquals(CategoryRequestStatus.PENDING, entity.getStatus());
    }

    @Test
    void create_sameNameFromAnotherRequester_stillSavesRequest() {
        User requester = user("another@example.com", "ROLE_ORGANIZER");
        CreateCategoryProposalRequest request = proposal();
        CategoryRequest entity = CategoryRequest.builder().build();
        CategoryRequestResponse expected = CategoryRequestResponse.builder()
                .status(CategoryRequestStatus.PENDING)
                .build();
        when(userRepository.findByEmail("another@example.com"))
                .thenReturn(Optional.of(requester));
        when(categoryRequestMapper.toEntity(request)).thenReturn(entity);
        when(categoryRequestRepository.saveAndFlush(entity)).thenReturn(entity);
        when(categoryRequestMapper.toResponse(entity)).thenReturn(expected);

        CategoryRequestResponse response = service.create("another@example.com", request);

        assertEquals(expected, response);
        assertEquals("nghệ thuật", entity.getNormalizedProposedName());
        verify(categoryRequestRepository).saveAndFlush(entity);
    }

    @Test
    void review_approved_createsDraftCategoryAndLinksIt() {
        User requester = user("organizer@example.com", "ROLE_ORGANIZER");
        User reviewer = user("admin@example.com", "ROLE_ADMIN");
        CategoryRequest entity = pendingRequest(requester);
        Category category = Category.builder()
                .name("Nghệ thuật")
                .slug("nghe-thuat")
                .status(CategoryStatus.DRAFT)
                .build();
        category.setId(UUID.randomUUID());
        ReviewCategoryRequest review = new ReviewCategoryRequest();
        review.setStatus("APPROVED");
        review.setSlug("nghe-thuat");
        CategoryResponse created = CategoryResponse.builder()
                .id(category.getId())
                .status(CategoryStatus.DRAFT)
                .build();
        CategoryRequestResponse expected = CategoryRequestResponse.builder()
                .status(CategoryRequestStatus.APPROVED)
                .build();
        when(categoryRequestRepository.findByIdForUpdate(entity.getId()))
                .thenReturn(Optional.of(entity));
        when(categoryService.create(any(CreateCategoryRequest.class))).thenReturn(created);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(reviewer));
        when(categoryRequestRepository.save(entity)).thenReturn(entity);
        when(categoryRequestMapper.toResponse(entity)).thenReturn(expected);

        CategoryRequestResponse response = service.review(
                entity.getId(),
                "admin@example.com",
                review);

        assertEquals(expected, response);
        assertEquals(CategoryRequestStatus.APPROVED, entity.getStatus());
        assertEquals(category, entity.getCategory());
        assertEquals(reviewer, entity.getReviewedBy());
        assertNotNull(entity.getReviewedAt());
        ArgumentCaptor<CreateCategoryRequest> captor =
                ArgumentCaptor.forClass(CreateCategoryRequest.class);
        verify(categoryService).create(captor.capture());
        assertEquals("Nghệ thuật", captor.getValue().getName());
        assertEquals("nghe-thuat", captor.getValue().getSlug());
    }

    @Test
    void review_duplicateName_linksExistingCategoryAndIgnoresDifferentSlug() {
        User requester = user("organizer@example.com", "ROLE_ORGANIZER");
        User reviewer = user("admin@example.com", "ROLE_ADMIN");
        CategoryRequest entity = pendingRequest(requester);
        Category existing = Category.builder()
                .name("Nghệ thuật")
                .normalizedName("nghệ thuật")
                .slug("nghe-thuat")
                .status(CategoryStatus.DRAFT)
                .build();
        ReviewCategoryRequest review = new ReviewCategoryRequest();
        review.setStatus("APPROVED");
        review.setSlug("su-kien-nghe-thuat");
        CategoryRequestResponse expected = CategoryRequestResponse.builder()
                .status(CategoryRequestStatus.APPROVED)
                .build();
        when(categoryRequestRepository.findByIdForUpdate(entity.getId()))
                .thenReturn(Optional.of(entity));
        when(categoryRepository.findFirstByNormalizedNameAndStatusIn(
                eq("nghệ thuật"),
                any())).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(reviewer));
        when(categoryRequestRepository.save(entity)).thenReturn(entity);
        when(categoryRequestMapper.toResponse(entity)).thenReturn(expected);

        CategoryRequestResponse response = service.review(
                entity.getId(),
                "admin@example.com",
                review);

        assertEquals(expected, response);
        assertEquals(existing, entity.getCategory());
        assertEquals(CategoryRequestStatus.APPROVED, entity.getStatus());
        verify(categoryService, never()).create(any(CreateCategoryRequest.class));
    }

    @Test
    void review_rejectedWithoutNote_throwsInvalidRequest() {
        CategoryRequest entity = pendingRequest(
                user("organizer@example.com", "ROLE_ORGANIZER"));
        ReviewCategoryRequest review = new ReviewCategoryRequest();
        review.setStatus("REJECTED");
        when(categoryRequestRepository.findByIdForUpdate(entity.getId()))
                .thenReturn(Optional.of(entity));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.review(entity.getId(), "admin@example.com", review));

        assertEquals(ErrorCode.INVALID_CATEGORY_REQUEST, exception.getErrorCode());
        verify(categoryRequestRepository, never()).save(any());
    }

    private CreateCategoryProposalRequest proposal() {
        CreateCategoryProposalRequest request = new CreateCategoryProposalRequest();
        request.setProposedName("Nghệ thuật");
        request.setDescription("Sự kiện triển lãm và mỹ thuật");
        return request;
    }

    private CategoryRequest pendingRequest(User requester) {
        CategoryRequest request = CategoryRequest.builder()
                .requester(requester)
                .proposedName("Nghệ thuật")
                .normalizedProposedName("nghệ thuật")
                .status(CategoryRequestStatus.PENDING)
                .build();
        request.setId(UUID.randomUUID());
        return request;
    }

    private User user(String email, String roleCode) {
        User user = User.builder()
                .email(email)
                .passwordHash("hash")
                .fullName("User")
                .role(Role.builder().code(roleCode).name(roleCode).build())
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }
}
