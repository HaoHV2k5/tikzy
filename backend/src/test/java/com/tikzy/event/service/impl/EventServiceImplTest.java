package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.storage.ImageStorageService;
import com.tikzy.common.storage.StoredImage;
import com.tikzy.event.dto.request.CreateEventRequest;
import com.tikzy.event.dto.request.SearchEventsRequest;
import com.tikzy.event.dto.request.UpdateEventRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.dto.response.PublicEventDetailResponse;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.mapper.EventMapper;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.ShowTimeRepository;
import com.tikzy.event.repository.TicketTypeRepository;
import com.tikzy.ticket.entity.ShowTimeTicketInventory;
import com.tikzy.ticket.repository.ShowTimeTicketInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    @Mock
    private ShowTimeRepository showTimeRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private ShowTimeTicketInventoryRepository inventoryRepository;

    private final EventMapper eventMapper = Mappers.getMapper(EventMapper.class);
    private EventServiceImpl eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventServiceImpl(
                eventRepository,
                categoryRepository,
                userRepository,
                eventMapper,
                imageStorageService,
                showTimeRepository,
                ticketTypeRepository,
                inventoryRepository);
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

    @Test
    void publish_draftEventWithInventory_setsPublishedStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        ShowTime showTime = showTime(event);
        TicketType ticketType = ticketType(event);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(event.getId()))
                .thenReturn(List.of(showTime));
        when(ticketTypeRepository.findAllByEventIdAndIsActiveTrue(event.getId()))
                .thenReturn(List.of(ticketType));
        when(inventoryRepository.findAllByShowTimeEventId(event.getId()))
                .thenReturn(List.of(inventory(showTime, ticketType, 100, 10, 20)));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventResponse response = eventService.publish("organizer@example.com", event.getId());

        assertEquals(EventStatus.PUBLISHED, response.getStatus());
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertEquals(EventStatus.PUBLISHED, captor.getValue().getStatus());
    }

    @Test
    void publish_withoutShowTime_throwsInvalidData() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(event.getId()))
                .thenReturn(List.of());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.publish("organizer@example.com", event.getId()));

        assertEquals(ErrorCode.INVALID_EVENT_DATA, exception.getErrorCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publish_withoutTicketType_throwsInvalidData() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        ShowTime showTime = showTime(event);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(event.getId()))
                .thenReturn(List.of(showTime));
        when(ticketTypeRepository.findAllByEventIdAndIsActiveTrue(event.getId()))
                .thenReturn(List.of());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.publish("organizer@example.com", event.getId()));

        assertEquals(ErrorCode.INVALID_EVENT_DATA, exception.getErrorCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publish_withoutInventory_throwsInvalidData() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        ShowTime showTime = showTime(event);
        TicketType ticketType = ticketType(event);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(event.getId()))
                .thenReturn(List.of(showTime));
        when(ticketTypeRepository.findAllByEventIdAndIsActiveTrue(event.getId()))
                .thenReturn(List.of(ticketType));
        when(inventoryRepository.findAllByShowTimeEventId(event.getId()))
                .thenReturn(List.of());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.publish("organizer@example.com", event.getId()));

        assertEquals(ErrorCode.INVALID_EVENT_DATA, exception.getErrorCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void publish_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer, publishedCategory());
        event.setStatus(EventStatus.PUBLISHED);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.publish("organizer@example.com", event.getId()));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void getPublished_returnsOnlyPublishedEventsWithoutOrganizerEmail() {
        Event event = draftEvent(organizer(), publishedCategory());
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findAllByStatus(eq(EventStatus.PUBLISHED), any()))
                .thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponse> page = eventService.getPublished(null, PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        assertEquals(event.getTitle(), page.getContent().getFirst().getTitle());
        assertEquals(EventStatus.PUBLISHED, page.getContent().getFirst().getStatus());
        assertNull(page.getContent().getFirst().getOrganizerEmail());
    }

    @Test
    void getPublished_byCategory_returnsMatchingPublishedEvents() {
        Category category = publishedCategory();
        Event event = draftEvent(organizer(), category);
        event.setStatus(EventStatus.PUBLISHED);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(eventRepository.findAllByCategoryIdAndStatus(
                eq(category.getId()),
                eq(EventStatus.PUBLISHED),
                any())).thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponse> page = eventService.getPublished(category.getId(), PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        assertEquals(event.getTitle(), page.getContent().getFirst().getTitle());
    }

    @Test
    void getPublished_unpublishedCategory_throwsNotFound() {
        Category category = publishedCategory();
        category.setStatus(CategoryStatus.DRAFT);
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.getPublished(category.getId(), PageRequest.of(0, 20)));

        assertEquals(ErrorCode.CATEGORY_NOT_FOUND, exception.getErrorCode());
        verify(eventRepository, never()).findAllByCategoryIdAndStatus(any(), any(), any());
    }

    @Test
    void searchPublished_blankFilters_returnsPublishedEventsWithoutOrganizerEmail() {
        Event event = draftEvent(organizer(), publishedCategory());
        event.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.searchPublished(
                eq(EventStatus.PUBLISHED),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                isNull(),
                any())).thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponse> page = eventService.searchPublished(new SearchEventsRequest(), PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        assertEquals(event.getTitle(), page.getContent().getFirst().getTitle());
        assertNull(page.getContent().getFirst().getOrganizerEmail());
    }

    @Test
    void searchPublished_normalizesKeywordLocationAndDateRange() {
        Event event = draftEvent(organizer(), publishedCategory());
        event.setStatus(EventStatus.PUBLISHED);
        SearchEventsRequest request = new SearchEventsRequest();
        request.setKeyword("  Hòa Nhạc  ");
        request.setLocation("  Hà Nội  ");
        request.setFrom(LocalDate.of(2026, 10, 1));
        request.setTo(LocalDate.of(2026, 10, 31));
        request.setMinPrice(new BigDecimal("100000"));
        request.setMaxPrice(new BigDecimal("2000000"));
        when(eventRepository.searchPublished(
                eq(EventStatus.PUBLISHED),
                eq("%hòa nhạc%"),
                eq("%hà nội%"),
                eq(LocalDate.of(2026, 10, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 10, 31).atTime(LocalTime.MAX)),
                eq(new BigDecimal("100000")),
                eq(new BigDecimal("2000000")),
                any())).thenReturn(new PageImpl<>(List.of(event)));

        Page<EventResponse> page = eventService.searchPublished(request, PageRequest.of(0, 20));

        assertEquals(1, page.getContent().size());
        verify(eventRepository).searchPublished(
                eq(EventStatus.PUBLISHED),
                eq("%hòa nhạc%"),
                eq("%hà nội%"),
                eq(LocalDate.of(2026, 10, 1).atStartOfDay()),
                eq(LocalDate.of(2026, 10, 31).atTime(LocalTime.MAX)),
                eq(new BigDecimal("100000")),
                eq(new BigDecimal("2000000")),
                any());
    }

    @Test
    void searchPublished_fromAfterTo_throwsInvalidData() {
        SearchEventsRequest request = new SearchEventsRequest();
        request.setFrom(LocalDate.of(2026, 10, 31));
        request.setTo(LocalDate.of(2026, 10, 1));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.searchPublished(request, PageRequest.of(0, 20)));

        assertEquals(ErrorCode.INVALID_EVENT_DATA, exception.getErrorCode());
        verify(eventRepository, never()).searchPublished(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchPublished_minPriceGreaterThanMax_throwsInvalidData() {
        SearchEventsRequest request = new SearchEventsRequest();
        request.setMinPrice(new BigDecimal("2000000"));
        request.setMaxPrice(new BigDecimal("100000"));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.searchPublished(request, PageRequest.of(0, 20)));

        assertEquals(ErrorCode.INVALID_EVENT_DATA, exception.getErrorCode());
        verify(eventRepository, never()).searchPublished(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchPublished_negativeMinPrice_throwsInvalidData() {
        SearchEventsRequest request = new SearchEventsRequest();
        request.setMinPrice(new BigDecimal("-1"));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.searchPublished(request, PageRequest.of(0, 20)));

        assertEquals(ErrorCode.INVALID_EVENT_DATA, exception.getErrorCode());
        verify(eventRepository, never()).searchPublished(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void getPublishedById_returnsShowTimesAndRemainingInventory() {
        Event event = draftEvent(organizer(), publishedCategory());
        event.setStatus(EventStatus.PUBLISHED);
        ShowTime showTime = showTime(event);
        TicketType ticketType = ticketType(event);
        when(eventRepository.findByIdAndStatus(event.getId(), EventStatus.PUBLISHED))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(event.getId()))
                .thenReturn(List.of(showTime));
        when(ticketTypeRepository.findAllByEventIdAndIsActiveTrue(event.getId()))
                .thenReturn(List.of(ticketType));
        when(inventoryRepository.findAllByShowTimeEventId(event.getId()))
                .thenReturn(List.of(inventory(showTime, ticketType, 100, 10, 20)));

        PublicEventDetailResponse response = eventService.getPublishedById(event.getId());

        assertEquals(event.getTitle(), response.getTitle());
        assertEquals(EventStatus.PUBLISHED, response.getStatus());
        assertEquals(1, response.getShowTimes().size());
        assertEquals(showTime.getId(), response.getShowTimes().getFirst().getId());
        assertEquals(1, response.getShowTimes().getFirst().getTicketOffers().size());
        assertEquals(70, response.getShowTimes().getFirst().getTicketOffers().getFirst().getAvailableQuantity());
        assertEquals(ticketType.getId(), response.getShowTimes().getFirst().getTicketOffers().getFirst().getTicketTypeId());
    }

    @Test
    void getPublishedById_draftEvent_throwsNotFound() {
        when(eventRepository.findByIdAndStatus(any(), eq(EventStatus.PUBLISHED)))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventService.getPublishedById(UUID.randomUUID()));

        assertEquals(ErrorCode.EVENT_NOT_FOUND, exception.getErrorCode());
    }

    private CreateEventRequest createRequest(UUID categoryId) {
        CreateEventRequest request = new CreateEventRequest();
        request.setCategoryId(categoryId);
        request.setTitle("Hòa nhạc mùa hè");
        request.setVenueName("Nhà hát lớn");
        return request;
    }

    private ShowTime showTime(Event event) {
        ShowTime showTime = ShowTime.builder()
                .event(event)
                .startTime(LocalDateTime.of(2026, 10, 15, 19, 0))
                .endTime(LocalDateTime.of(2026, 10, 15, 22, 0))
                .isActive(true)
                .build();
        showTime.setId(UUID.randomUUID());
        return showTime;
    }

    private TicketType ticketType(Event event) {
        TicketType ticketType = TicketType.builder()
                .event(event)
                .name("VIP")
                .price(new BigDecimal("1500000"))
                .maxPerOrder(4)
                .isActive(true)
                .build();
        ticketType.setId(UUID.randomUUID());
        return ticketType;
    }

    private ShowTimeTicketInventory inventory(
            ShowTime showTime,
            TicketType ticketType,
            int total,
            int reserved,
            int sold) {
        ShowTimeTicketInventory inventory = ShowTimeTicketInventory.builder()
                .showTime(showTime)
                .ticketType(ticketType)
                .totalQuantity(total)
                .reservedQuantity(reserved)
                .soldQuantity(sold)
                .build();
        inventory.setId(UUID.randomUUID());
        return inventory;
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
