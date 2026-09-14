package com.tikzy.ticket.service.impl;

import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.ShowTimeRepository;
import com.tikzy.event.repository.TicketTypeRepository;
import com.tikzy.ticket.dto.request.InitializeInventoryRequest;
import com.tikzy.ticket.dto.request.UpdateInventoryRequest;
import com.tikzy.ticket.dto.request.UpsertInventoriesRequest;
import com.tikzy.ticket.dto.request.UpsertInventoryItemRequest;
import com.tikzy.ticket.dto.response.InventoryResponse;
import com.tikzy.ticket.entity.ShowTimeTicketInventory;
import com.tikzy.ticket.mapper.InventoryMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private ShowTimeRepository showTimeRepository;
    @Mock
    private TicketTypeRepository ticketTypeRepository;
    @Mock
    private ShowTimeTicketInventoryRepository inventoryRepository;
    @Mock
    private UserRepository userRepository;

    private final InventoryMapper inventoryMapper = Mappers.getMapper(InventoryMapper.class);
    private InventoryServiceImpl inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryServiceImpl(
                eventRepository,
                showTimeRepository,
                ticketTypeRepository,
                inventoryRepository,
                userRepository,
                inventoryMapper);
    }

    @Test
    void ensurePairsForShowTime_createsMissingTicketTypeRows() {
        Event event = draftEvent(organizer());
        ShowTime showTime = showTime(event);
        TicketType ticketType = ticketType(event, "VIP");
        when(ticketTypeRepository.findAllByEventIdOrderByCreatedAtAsc(event.getId()))
                .thenReturn(List.of(ticketType));
        when(inventoryRepository.existsByShowTimeIdAndTicketTypeId(showTime.getId(), ticketType.getId()))
                .thenReturn(false);

        inventoryService.ensurePairsForShowTime(showTime);

        ArgumentCaptor<ShowTimeTicketInventory> captor = ArgumentCaptor.forClass(ShowTimeTicketInventory.class);
        verify(inventoryRepository).save(captor.capture());
        assertEquals(showTime, captor.getValue().getShowTime());
        assertEquals(ticketType, captor.getValue().getTicketType());
        assertEquals(0, captor.getValue().getTotalQuantity());
    }

    @Test
    void ensurePairsForTicketType_skipsExistingPair() {
        Event event = draftEvent(organizer());
        ShowTime showTime = showTime(event);
        TicketType ticketType = ticketType(event, "GA");
        when(showTimeRepository.findAllByEventIdOrderByStartTimeAsc(event.getId()))
                .thenReturn(List.of(showTime));
        when(inventoryRepository.existsByShowTimeIdAndTicketTypeId(showTime.getId(), ticketType.getId()))
                .thenReturn(true);

        inventoryService.ensurePairsForTicketType(ticketType);

        verify(inventoryRepository, never()).save(any(ShowTimeTicketInventory.class));
    }

    @Test
    void initialize_createsCartesianProductForOwnDraft() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTime firstShow = showTime(event);
        ShowTime secondShow = showTime(event);
        TicketType vip = ticketType(event, "VIP");
        TicketType ga = ticketType(event, "GA");
        stubOwnEvent(organizer, event);
        when(showTimeRepository.findAllByEventIdOrderByStartTimeAsc(event.getId()))
                .thenReturn(List.of(firstShow, secondShow));
        when(ticketTypeRepository.findAllByEventIdOrderByCreatedAtAsc(event.getId()))
                .thenReturn(List.of(vip, ga));
        when(inventoryRepository.existsByShowTimeIdAndTicketTypeId(any(), any())).thenReturn(false);
        when(inventoryRepository.save(any(ShowTimeTicketInventory.class))).thenAnswer(invocation -> {
            ShowTimeTicketInventory inventory = invocation.getArgument(0);
            inventory.setId(UUID.randomUUID());
            return inventory;
        });
        when(inventoryRepository.findAllByShowTimeEventId(event.getId())).thenReturn(List.of());

        InitializeInventoryRequest request = new InitializeInventoryRequest();
        request.setDefaultTotalQuantity(50);
        inventoryService.initialize("organizer@example.com", event.getId(), request);

        verify(inventoryRepository, times(4)).save(any(ShowTimeTicketInventory.class));
    }

    @Test
    void initialize_publishedEvent_throwsInvalidStatus() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        event.setStatus(EventStatus.PUBLISHED);
        stubOwnEvent(organizer, event);

        AppException exception = assertThrows(
                AppException.class,
                () -> inventoryService.initialize("organizer@example.com", event.getId(), null));

        assertEquals(ErrorCode.INVALID_EVENT_STATUS, exception.getErrorCode());
        verify(inventoryRepository, never()).save(any(ShowTimeTicketInventory.class));
    }

    @Test
    void upsert_createsPairWithQuantity() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTime showTime = showTime(event);
        TicketType ticketType = ticketType(event, "VIP");
        stubOwnEvent(organizer, event);
        when(showTimeRepository.findByIdAndEventId(showTime.getId(), event.getId()))
                .thenReturn(Optional.of(showTime));
        when(ticketTypeRepository.findByIdAndEventId(ticketType.getId(), event.getId()))
                .thenReturn(Optional.of(ticketType));
        when(inventoryRepository.findByShowTimeIdAndTicketTypeId(showTime.getId(), ticketType.getId()))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(ShowTimeTicketInventory.class))).thenAnswer(invocation -> {
            ShowTimeTicketInventory inventory = invocation.getArgument(0);
            inventory.setId(UUID.randomUUID());
            return inventory;
        });

        UpsertInventoryItemRequest item = new UpsertInventoryItemRequest();
        item.setShowTimeId(showTime.getId());
        item.setTicketTypeId(ticketType.getId());
        item.setTotalQuantity(120);
        UpsertInventoriesRequest request = new UpsertInventoriesRequest();
        request.setItems(List.of(item));

        List<InventoryResponse> responses = inventoryService.upsert(
                "organizer@example.com",
                event.getId(),
                request);

        assertEquals(1, responses.size());
        assertEquals(120, responses.getFirst().getTotalQuantity());
        assertEquals(120, responses.getFirst().getAvailableQuantity());
        assertEquals("VIP", responses.getFirst().getTicketTypeName());
    }

    @Test
    void upsert_otherEventShowTime_throwsNotFound() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        stubOwnEvent(organizer, event);
        when(showTimeRepository.findByIdAndEventId(any(), eq(event.getId()))).thenReturn(Optional.empty());

        UpsertInventoryItemRequest item = new UpsertInventoryItemRequest();
        item.setShowTimeId(UUID.randomUUID());
        item.setTicketTypeId(UUID.randomUUID());
        item.setTotalQuantity(10);
        UpsertInventoriesRequest request = new UpsertInventoriesRequest();
        request.setItems(List.of(item));

        AppException exception = assertThrows(
                AppException.class,
                () -> inventoryService.upsert("organizer@example.com", event.getId(), request));

        assertEquals(ErrorCode.SHOW_TIME_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void update_rejectsQuantityBelowReservedAndSold() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTimeTicketInventory inventory = inventory(showTime(event), ticketType(event, "GA"));
        inventory.setReservedQuantity(5);
        inventory.setSoldQuantity(10);
        stubOwnEvent(organizer, event);
        when(inventoryRepository.findByIdAndShowTimeEventId(inventory.getId(), event.getId()))
                .thenReturn(Optional.of(inventory));

        UpdateInventoryRequest request = new UpdateInventoryRequest();
        request.setTotalQuantity(12);

        AppException exception = assertThrows(
                AppException.class,
                () -> inventoryService.update(
                        "organizer@example.com",
                        event.getId(),
                        inventory.getId(),
                        request));

        assertEquals(ErrorCode.INVALID_INVENTORY_DATA, exception.getErrorCode());
        verify(inventoryRepository, never()).save(any(ShowTimeTicketInventory.class));
    }

    @Test
    void getOwn_filtersByShowTimeAndTicketType() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        ShowTime showTime = showTime(event);
        TicketType ticketType = ticketType(event, "VIP");
        ShowTimeTicketInventory inventory = inventory(showTime, ticketType);
        stubOwnEvent(organizer, event);
        when(inventoryRepository.findAllByShowTimeEventIdAndShowTimeIdAndTicketTypeId(
                eq(event.getId()),
                eq(showTime.getId()),
                eq(ticketType.getId()),
                any()))
                .thenReturn(new PageImpl<>(List.of(inventory)));

        Page<InventoryResponse> page = inventoryService.getOwn(
                "organizer@example.com",
                event.getId(),
                showTime.getId(),
                ticketType.getId(),
                PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements());
        assertEquals(inventory.getId(), page.getContent().getFirst().getId());
        assertEquals(100, page.getContent().getFirst().getAvailableQuantity());
    }

    @Test
    void getOwnById_missing_throwsNotFound() {
        User organizer = organizer();
        Event event = draftEvent(organizer);
        stubOwnEvent(organizer, event);
        when(inventoryRepository.findByIdAndShowTimeEventId(any(), eq(event.getId())))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> inventoryService.getOwnById(
                        "organizer@example.com",
                        event.getId(),
                        UUID.randomUUID()));

        assertEquals(ErrorCode.INVENTORY_NOT_FOUND, exception.getErrorCode());
    }

    private void stubOwnEvent(User organizer, Event event) {
        when(userRepository.findByEmail("organizer@example.com")).thenReturn(Optional.of(organizer));
        when(eventRepository.findByIdAndOrganizerId(event.getId(), organizer.getId()))
                .thenReturn(Optional.of(event));
    }

    private ShowTimeTicketInventory inventory(ShowTime showTime, TicketType ticketType) {
        ShowTimeTicketInventory inventory = ShowTimeTicketInventory.builder()
                .showTime(showTime)
                .ticketType(ticketType)
                .totalQuantity(100)
                .reservedQuantity(0)
                .soldQuantity(0)
                .build();
        inventory.setId(UUID.randomUUID());
        return inventory;
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

    private TicketType ticketType(Event event, String name) {
        TicketType ticketType = TicketType.builder()
                .event(event)
                .name(name)
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
