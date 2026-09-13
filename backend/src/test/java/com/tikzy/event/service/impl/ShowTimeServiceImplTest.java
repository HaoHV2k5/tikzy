package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.dto.request.CreateShowTimeRequest;
import com.tikzy.event.dto.request.UpdateShowTimeRequest;
import com.tikzy.event.dto.response.ShowTimeResponse;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.mapper.ShowTimeMapper;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.ShowTimeRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShowTimeServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private ShowTimeRepository showTimeRepository;
    @Mock
    private UserRepository userRepository;

    private final ShowTimeMapper showTimeMapper = Mappers.getMapper(ShowTimeMapper.class);
    private ShowTimeServiceImpl showTimeService;

    @BeforeEach
    void setUp() {
        showTimeService = new ShowTimeServiceImpl(
                eventRepository,
                showTimeRepository,
                userRepository,
                showTimeMapper);
    }

    @Test
    void create_savesActiveShowTimeForOwnDraft() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        CreateShowTimeRequest request = createRequest();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.existsOverlapping(event.getId(), request.getStartTime(), request.getEndTime()))
                .thenReturn(false);
        when(showTimeRepository.save(any(ShowTime.class))).thenAnswer(invocation -> {
            ShowTime showTime = invocation.getArgument(0);
            showTime.setId(UUID.randomUUID());
            showTime.setCreatedAt(LocalDateTime.now());
            return showTime;
        });

        ShowTimeResponse response = showTimeService.create("organizer@example.com", event.getId(), request);

        assertEquals(event.getId(), response.getEventId());
        assertEquals(request.getStartTime(), response.getStartTime());
        assertEquals(request.getEndTime(), response.getEndTime());
        assertTrue(response.getIsActive());
        ArgumentCaptor<ShowTime> captor = ArgumentCaptor.forClass(ShowTime.class);
        verify(showTimeRepository).save(captor.capture());
        assertEquals(event, captor.getValue().getEvent());
        assertTrue(captor.getValue().getIsActive());
    }

    @Test
    void create_endBeforeStart_throwsInvalidData() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        CreateShowTimeRequest request = createRequest();
        request.setEndTime(request.getStartTime().minusHours(1));
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> showTimeService.create("organizer@example.com", event.getId(), request));

        assertEquals(ErrorCode.INVALID_SHOW_TIME_DATA, exception.getErrorCode());
        verify(showTimeRepository, never()).save(any(ShowTime.class));
    }

    @Test
    void create_overlappingShowTime_throwsInvalidData() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        CreateShowTimeRequest request = createRequest();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.existsOverlapping(event.getId(), request.getStartTime(), request.getEndTime()))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> showTimeService.create("organizer@example.com", event.getId(), request));

        assertEquals(ErrorCode.INVALID_SHOW_TIME_DATA, exception.getErrorCode());
        verify(showTimeRepository, never()).save(any(ShowTime.class));
    }

    @Test
    void create_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        event.setStatus(EventStatus.PUBLISHED);
        CreateShowTimeRequest request = createRequest();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> showTimeService.create("organizer@example.com", event.getId(), request));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(showTimeRepository, never()).save(any(ShowTime.class));
    }

    @Test
    void create_otherOrganizer_throwsEventNotFound() {
        User organizer = organizer();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(any(), any())).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> showTimeService.create(
                        "organizer@example.com",
                        UUID.randomUUID(),
                        createRequest()));

        assertEquals(ErrorCode.EVENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void update_changesTimesForOwnDraft() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTime showTime = showTime(event);
        UpdateShowTimeRequest request = new UpdateShowTimeRequest();
        request.setStartTime(LocalDateTime.of(2026, 10, 16, 18, 0));
        request.setEndTime(LocalDateTime.of(2026, 10, 16, 21, 0));
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findByIdAndEventId(showTime.getId(), event.getId()))
                .thenReturn(Optional.of(showTime));
        when(showTimeRepository.existsOverlappingExcludingId(
                event.getId(),
                request.getStartTime(),
                request.getEndTime(),
                showTime.getId()))
                .thenReturn(false);
        when(showTimeRepository.save(any(ShowTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShowTimeResponse response = showTimeService.update(
                "organizer@example.com",
                event.getId(),
                showTime.getId(),
                request);

        assertEquals(request.getStartTime(), response.getStartTime());
        assertEquals(request.getEndTime(), response.getEndTime());
        verify(showTimeRepository).save(showTime);
    }

    @Test
    void update_deactivateSkipsOverlapCheck() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTime showTime = showTime(event);
        UpdateShowTimeRequest request = new UpdateShowTimeRequest();
        request.setIsActive(false);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findByIdAndEventId(showTime.getId(), event.getId()))
                .thenReturn(Optional.of(showTime));
        when(showTimeRepository.save(any(ShowTime.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShowTimeResponse response = showTimeService.update(
                "organizer@example.com",
                event.getId(),
                showTime.getId(),
                request);

        assertFalse(response.getIsActive());
        verify(showTimeRepository, never()).existsOverlappingExcludingId(any(), any(), any(), any());
    }

    @Test
    void update_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        event.setStatus(EventStatus.PUBLISHED);
        ShowTime showTime = showTime(event);
        UpdateShowTimeRequest request = new UpdateShowTimeRequest();
        request.setIsActive(false);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findByIdAndEventId(showTime.getId(), event.getId()))
                .thenReturn(Optional.of(showTime));

        AppException exception = assertThrows(
                AppException.class,
                () -> showTimeService.update(
                        "organizer@example.com",
                        event.getId(),
                        showTime.getId(),
                        request));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(showTimeRepository, never()).save(any(ShowTime.class));
    }

    @Test
    void getOwnById_wrongShowTime_throwsNotFound() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findByIdAndEventId(any(), eq(event.getId()))).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> showTimeService.getOwnById(
                        "organizer@example.com",
                        event.getId(),
                        UUID.randomUUID()));

        assertEquals(ErrorCode.SHOW_TIME_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getOwn_filtersByActive() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTime showTime = showTime(event);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findAllByEventIdAndIsActive(eq(event.getId()), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(showTime)));

        Page<ShowTimeResponse> page = showTimeService.getOwn(
                "organizer@example.com",
                event.getId(),
                true,
                PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(showTime.getId(), page.getContent().getFirst().getId());
        verify(showTimeRepository).findAllByEventIdAndIsActive(eq(event.getId()), eq(true), any());
    }

    @Test
    void deleteDraft_removesOwnShowTime() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTime showTime = showTime(event);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(showTimeRepository.findByIdAndEventId(showTime.getId(), event.getId()))
                .thenReturn(Optional.of(showTime));

        ShowTimeResponse response = showTimeService.deleteDraft(
                "organizer@example.com",
                event.getId(),
                showTime.getId());

        assertEquals(showTime.getId(), response.getId());
        verify(showTimeRepository).delete(showTime);
    }

    private CreateShowTimeRequest createRequest() {
        CreateShowTimeRequest request = new CreateShowTimeRequest();
        request.setStartTime(LocalDateTime.of(2026, 10, 15, 19, 0));
        request.setEndTime(LocalDateTime.of(2026, 10, 15, 22, 0));
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

    private Event draftEvent(User organizer) {
        Event event = Event.builder()
                .organizer(organizer)
                .title("Hòa nhạc mùa hè")
                .status(EventStatus.DRAFT)
                .refundPolicy(RefundPolicy.NO_REFUND)
                .build();
        event.setId(UUID.randomUUID());
        return event;
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
