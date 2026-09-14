package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.dto.request.CreateTicketTypeRequest;
import com.tikzy.event.dto.request.UpdateTicketTypeRequest;
import com.tikzy.event.dto.response.TicketTypeResponse;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.TicketType;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.mapper.TicketTypeMapper;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.TicketTypeRepository;
import com.tikzy.ticket.service.InventoryService;
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

import java.math.BigDecimal;
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
class TicketTypeServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryService inventoryService;

    private final TicketTypeMapper ticketTypeMapper = Mappers.getMapper(TicketTypeMapper.class);
    private TicketTypeServiceImpl ticketTypeService;

    @BeforeEach
    void setUp() {
        ticketTypeService = new TicketTypeServiceImpl(
                eventRepository,
                ticketTypeRepository,
                userRepository,
                ticketTypeMapper,
                inventoryService);
    }

    @Test
    void create_savesActiveTicketTypeForOwnDraft() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        CreateTicketTypeRequest request = createRequest();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.existsByEventIdAndNameIgnoreCase(event.getId(), "VIP"))
                .thenReturn(false);
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> {
            TicketType ticketType = invocation.getArgument(0);
            ticketType.setId(UUID.randomUUID());
            ticketType.setCreatedAt(LocalDateTime.now());
            return ticketType;
        });

        TicketTypeResponse response = ticketTypeService.create("organizer@example.com", event.getId(), request);

        assertEquals(event.getId(), response.getEventId());
        assertEquals("VIP", response.getName());
        assertEquals(0, new BigDecimal("1500000").compareTo(response.getPrice()));
        assertEquals(4, response.getMaxPerOrder());
        assertTrue(response.getIsActive());
        ArgumentCaptor<TicketType> captor = ArgumentCaptor.forClass(TicketType.class);
        verify(ticketTypeRepository).save(captor.capture());
        assertEquals(event, captor.getValue().getEvent());
        assertTrue(captor.getValue().getIsActive());
        verify(inventoryService).ensurePairsForTicketType(captor.getValue());
    }

    @Test
    void create_withoutMaxPerOrder_defaultsToTen() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        CreateTicketTypeRequest request = createRequest();
        request.setMaxPerOrder(null);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.existsByEventIdAndNameIgnoreCase(event.getId(), "VIP"))
                .thenReturn(false);
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketTypeResponse response = ticketTypeService.create("organizer@example.com", event.getId(), request);

        assertEquals(10, response.getMaxPerOrder());
    }

    @Test
    void create_duplicateName_throwsInvalidData() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        CreateTicketTypeRequest request = createRequest();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.existsByEventIdAndNameIgnoreCase(event.getId(), "VIP"))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> ticketTypeService.create("organizer@example.com", event.getId(), request));

        assertEquals(ErrorCode.INVALID_TICKET_TYPE_DATA, exception.getErrorCode());
        verify(ticketTypeRepository, never()).save(any(TicketType.class));
    }

    @Test
    void create_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        event.setStatus(EventStatus.PUBLISHED);
        CreateTicketTypeRequest request = createRequest();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));

        AppException exception = assertThrows(
                AppException.class,
                () -> ticketTypeService.create("organizer@example.com", event.getId(), request));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(ticketTypeRepository, never()).save(any(TicketType.class));
    }

    @Test
    void create_otherOrganizer_throwsEventNotFound() {
        User organizer = organizer();
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(any(), any())).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> ticketTypeService.create(
                        "organizer@example.com",
                        UUID.randomUUID(),
                        createRequest()));

        assertEquals(ErrorCode.EVENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void update_changesPriceLimitAndSalesStatusForOwnDraft() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        TicketType ticketType = ticketType(event);
        UpdateTicketTypeRequest request = new UpdateTicketTypeRequest();
        request.setPrice(new BigDecimal("1800000"));
        request.setMaxPerOrder(2);
        request.setIsActive(false);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.findByIdAndEventId(ticketType.getId(), event.getId()))
                .thenReturn(Optional.of(ticketType));
        when(ticketTypeRepository.save(any(TicketType.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketTypeResponse response = ticketTypeService.update(
                "organizer@example.com",
                event.getId(),
                ticketType.getId(),
                request);

        assertEquals(0, new BigDecimal("1800000").compareTo(response.getPrice()));
        assertEquals(2, response.getMaxPerOrder());
        assertFalse(response.getIsActive());
        verify(ticketTypeRepository).save(ticketType);
    }

    @Test
    void update_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        event.setStatus(EventStatus.PUBLISHED);
        TicketType ticketType = ticketType(event);
        UpdateTicketTypeRequest request = new UpdateTicketTypeRequest();
        request.setIsActive(false);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.findByIdAndEventId(ticketType.getId(), event.getId()))
                .thenReturn(Optional.of(ticketType));

        AppException exception = assertThrows(
                AppException.class,
                () -> ticketTypeService.update(
                        "organizer@example.com",
                        event.getId(),
                        ticketType.getId(),
                        request));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(ticketTypeRepository, never()).save(any(TicketType.class));
    }

    @Test
    void getOwnById_wrongTicketType_throwsNotFound() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.findByIdAndEventId(any(), eq(event.getId()))).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> ticketTypeService.getOwnById(
                        "organizer@example.com",
                        event.getId(),
                        UUID.randomUUID()));

        assertEquals(ErrorCode.TICKET_TYPE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getOwn_filtersByActive() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        TicketType ticketType = ticketType(event);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.findAllByEventIdAndIsActive(eq(event.getId()), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of(ticketType)));

        Page<TicketTypeResponse> page = ticketTypeService.getOwn(
                "organizer@example.com",
                event.getId(),
                true,
                PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(ticketType.getId(), page.getContent().getFirst().getId());
        verify(ticketTypeRepository).findAllByEventIdAndIsActive(eq(event.getId()), eq(true), any());
    }

    @Test
    void deleteDraft_removesOwnTicketType() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        TicketType ticketType = ticketType(event);
        when(userRepository.findByEmail("organizer@example.com"))
                .thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
        when(ticketTypeRepository.findByIdAndEventId(ticketType.getId(), event.getId()))
                .thenReturn(Optional.of(ticketType));

        TicketTypeResponse response = ticketTypeService.deleteDraft(
                "organizer@example.com",
                event.getId(),
                ticketType.getId());

        assertEquals(ticketType.getId(), response.getId());
        verify(ticketTypeRepository).delete(ticketType);
    }

    private CreateTicketTypeRequest createRequest() {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest();
        request.setName("  VIP ");
        request.setPrice(new BigDecimal("1500000"));
        request.setMaxPerOrder(4);
        return request;
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
