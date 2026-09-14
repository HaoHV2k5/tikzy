package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.storage.ImageStorageService;
import com.tikzy.common.storage.StoredImage;
import com.tikzy.event.dto.request.CreateEventRequest;
import com.tikzy.event.dto.request.UpdateEventRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.Event;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.mapper.EventMapper;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
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
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ImageStorageService imageStorageService;

    private final EventMapper eventMapper = Mappers.getMapper(EventMapper.class);
    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(
                eventRepository,
                categoryRepository,
                userRepository,
                eventMapper,
                imageStorageService);
    }

    @Test
    void create_setsDraftStatusAndDefaults() {
        User organizer = organizer();
        Category category = publishedCategory();
        CreateEventRequest request = createRequest(category.getId());
        request.setTitle("  Hòa nhạc mùa hè  ");
        request.setVenueName("   ");
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(UUID.randomUUID());
            event.setCreatedAt(LocalDateTime.now());
            return event;
        });

        EventResponse response = eventService.create("organizer@example.com", request);

        assertEquals("Hòa nhạc mùa hè", response.getTitle());
        assertEquals(EventStatus.DRAFT, response.getStatus());
        assertEquals(RefundPolicy.NO_REFUND, response.getRefundPolicy());
        assertNull(response.getVenueName());
        assertEquals(organizer.getId(), response.getOrganizerId());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventStatus.DRAFT, captor.getValue().getStatus());
    }

    @Test
    void create_unpublishedCategory_throwsInvalidStatus() {
        User organizer = organizer();
        Category category = publishedCategory();
        category.setStatus(CategoryStatus.DRAFT);
        CreateEventRequest request = createRequest(category.getId());
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.create("organizer@example.com", request));

        assertEquals(ErrorCode.INVALID_CATEGORY_STATUS, exception.getErrorCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_allowRefundWithoutDeadline_throwsInvalidData() {
        User organizer = organizer();
        Category category = publishedCategory();
        CreateEventRequest request = createRequest(category.getId());
        request.setRefundPolicy(RefundPolicy.ALLOW_REFUND);
        request.setRefundFeePercentage(new BigDecimal("10.00"));
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.create("organizer@example.com", request));

        assertEquals(ErrorCode.INVALID_EVENT_DATA, exception.getErrorCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void update_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        event.setStatus(EventStatus.PUBLISHED);
        UpdateEventRequest request = new UpdateEventRequest();
        request.setTitle("Tên mới");
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.update("organizer@example.com", event.getId(), request));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void deleteDraft_removesOwnDraft() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        EventResponse response = eventService.deleteDraft("organizer@example.com", event.getId());

        assertEquals(event.getTitle(), response.getTitle());
        verify(eventRepository).delete(event);
        verify(imageStorageService, never()).delete(any());
    }

    @Test
    void uploadImage_storesBannerUrlFromCloudinary() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(imageStorageService.upload(any(), eq("events/" + event.getId() + "/banner")))
                .thenReturn(new StoredImage(
                        "https://res.cloudinary.com/demo/image/upload/v1/tikzy/events/"
                                + event.getId()
                                + "/banner.jpg",
                        "tikzy/events/" + event.getId() + "/banner"));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.uploadImage(
                "organizer@example.com",
                event.getId(),
                "banner",
                file);

        assertEquals(
                "https://res.cloudinary.com/demo/image/upload/v1/tikzy/events/"
                        + event.getId()
                        + "/banner.jpg",
                response.getBannerUrl());
        verify(imageStorageService).upload(any(), eq("events/" + event.getId() + "/banner"));
    }

    @Test
    void uploadImage_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        event.setStatus(EventStatus.PUBLISHED);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.uploadImage(
                        "organizer@example.com",
                        event.getId(),
                        "banner",
                        file));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(imageStorageService, never()).upload(any(), any());
    }

    @Test
    void uploadImage_invalidType_throwsInvalidImage() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3});
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.uploadImage(
                        "organizer@example.com",
                        event.getId(),
                        "poster",
                        file));

        assertEquals(ErrorCode.INVALID_EVENT_IMAGE, exception.getErrorCode());
        verify(imageStorageService, never()).upload(any(), any());
    }

    @Test
    void uploadImage_svg_throwsInvalidImage() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "banner.svg",
                "image/svg+xml",
                "<svg></svg>".getBytes());
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.uploadImage(
                        "organizer@example.com",
                        event.getId(),
                        "banner",
                        file));

        assertEquals(ErrorCode.INVALID_EVENT_IMAGE, exception.getErrorCode());
        verify(imageStorageService, never()).upload(any(), any());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void deleteImage_removesThumbnailFromCloudinary() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        event.setThumbnailUrl("https://res.cloudinary.com/demo/image/upload/v1/thumb.jpg");
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.deleteImage(
                "organizer@example.com",
                event.getId(),
                "thumbnail");

        assertNull(response.getThumbnailUrl());
        verify(imageStorageService).delete("events/" + event.getId() + "/thumbnail");
    }

    @Test
    void deleteImage_withoutExistingImage_throwsNotFound() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.deleteImage("organizer@example.com", event.getId(), "banner"));

        assertEquals(ErrorCode.EVENT_IMAGE_NOT_FOUND, exception.getErrorCode());
        verify(imageStorageService, never()).delete(any());
    }

    @Test
    void deleteDraft_deletesStoredImages() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        event.setBannerUrl("https://res.cloudinary.com/demo/image/upload/v1/banner.jpg");
        event.setThumbnailUrl("https://res.cloudinary.com/demo/image/upload/v1/thumb.jpg");
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        eventService.deleteDraft("organizer@example.com", event.getId());

        verify(imageStorageService).delete("events/" + event.getId() + "/banner");
        verify(imageStorageService).delete("events/" + event.getId() + "/thumbnail");
        verify(eventRepository).delete(event);
    }

    @Test
    void getOwnById_otherOrganizer_throwsNotFound() {
        User organizer = organizer();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(any(), any())).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.getOwnById("organizer@example.com", UUID.randomUUID()));

        assertEquals(ErrorCode.EVENT_NOT_FOUND, exception.getErrorCode());
    }

    private CreateEventRequest createRequest(UUID categoryId) {
        CreateEventRequest request = new CreateEventRequest();
        request.setCategoryId(categoryId);
        request.setTitle("Hòa nhạc mùa hè");
        request.setVenueName("Nhà hát lớn");
        return request;
    }

    private Event draftEvent(User organizer, Category category) {
        Event event = Event.builder()
                .organizer(organizer)
                .category(category)
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.DRAFT)
                .refundPolicy(RefundPolicy.NO_REFUND)
                .build();
        event.setId(UUID.randomUUID());
        return event;
    }

    private Category publishedCategory() {
        Category category = Category.builder()
                .name("Âm nhạc")
                .normalizedName("âm nhạc")
                .slug("am-nhac")
                .status(CategoryStatus.PUBLISHED)
                .sortOrder(0)
                .build();
        category.setId(UUID.randomUUID());
        return category;
    }

    private User organizer() {
        User user = User.builder()
                .email("organizer@example.com")
                .passwordHash("hash")
                .fullName("Ban tổ chức")
                .role(Role.builder().code("ROLE_ORGANIZER").name("Organizer").build())
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }
}
