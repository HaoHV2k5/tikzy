package com.tikzy.ticket.service.impl;

import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.event.enums.EventStatus;
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
import com.tikzy.ticket.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final EventRepository eventRepository;
    private final ShowTimeRepository showTimeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final ShowTimeTicketInventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final InventoryMapper inventoryMapper;

    @Override
    @Transactional
    public void ensurePairsForShowTime(ShowTime showTime) {
        List<TicketType> ticketTypes = ticketTypeRepository.findAllByEventIdOrderByCreatedAtAsc(
                showTime.getEvent().getId());
        ensurePairs(List.of(showTime), ticketTypes, 0);
    }

    @Override
    @Transactional
    public void ensurePairsForTicketType(TicketType ticketType) {
        List<ShowTime> showTimes = showTimeRepository.findAllByEventIdOrderByStartTimeAsc(
                ticketType.getEvent().getId());
        ensurePairs(showTimes, List.of(ticketType), 0);
    }

    @Override
    @Transactional
    public void ensurePairsForEvent(Event event, int defaultTotalQuantity) {
        validateTotalQuantity(defaultTotalQuantity, 0, 0);
        List<ShowTime> showTimes = showTimeRepository.findAllByEventIdOrderByStartTimeAsc(event.getId());
        List<TicketType> ticketTypes = ticketTypeRepository.findAllByEventIdOrderByCreatedAtAsc(event.getId());
        ensurePairs(showTimes, ticketTypes, defaultTotalQuantity);
    }

    @Override
    @Transactional
    public List<InventoryResponse> initialize(
            String organizerEmail,
            UUID eventId,
            InitializeInventoryRequest request) {
        Event event = findOwnDraftEvent(organizerEmail, eventId);
        int defaultTotalQuantity = request == null || request.getDefaultTotalQuantity() == null
                ? 0
                : request.getDefaultTotalQuantity();
        ensurePairsForEvent(event, defaultTotalQuantity);
        return inventoryRepository.findAllByShowTimeEventId(event.getId()).stream()
                .map(inventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<InventoryResponse> upsert(
            String organizerEmail,
            UUID eventId,
            UpsertInventoriesRequest request) {
        Event event = findOwnDraftEvent(organizerEmail, eventId);
        List<InventoryResponse> responses = new ArrayList<>();
        for (UpsertInventoryItemRequest item : request.getItems()) {
            ShowTime showTime = showTimeRepository.findByIdAndEventId(item.getShowTimeId(), event.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.SHOW_TIME_NOT_FOUND));
            TicketType ticketType = ticketTypeRepository.findByIdAndEventId(item.getTicketTypeId(), event.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND));
            ShowTimeTicketInventory inventory = inventoryRepository
                    .findByShowTimeIdAndTicketTypeId(showTime.getId(), ticketType.getId())
                    .orElseGet(() -> ShowTimeTicketInventory.builder()
                            .showTime(showTime)
                            .ticketType(ticketType)
                            .reservedQuantity(0)
                            .soldQuantity(0)
                            .build());
            applyTotalQuantity(inventory, item.getTotalQuantity());
            responses.add(inventoryMapper.toResponse(inventoryRepository.save(inventory)));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryResponse> getOwn(
            String organizerEmail,
            UUID eventId,
            UUID showTimeId,
            UUID ticketTypeId,
            Pageable pageable) {
        Event event = findOwnEvent(organizerEmail, eventId);
        Page<ShowTimeTicketInventory> inventories;
        if (showTimeId != null && ticketTypeId != null) {
            inventories = inventoryRepository.findAllByShowTimeEventIdAndShowTimeIdAndTicketTypeId(
                    event.getId(),
                    showTimeId,
                    ticketTypeId,
                    pageable);
        } else if (showTimeId != null) {
            inventories = inventoryRepository.findAllByShowTimeEventIdAndShowTimeId(
                    event.getId(),
                    showTimeId,
                    pageable);
        } else if (ticketTypeId != null) {
            inventories = inventoryRepository.findAllByShowTimeEventIdAndTicketTypeId(
                    event.getId(),
                    ticketTypeId,
                    pageable);
        } else {
            inventories = inventoryRepository.findAllByShowTimeEventId(event.getId(), pageable);
        }
        return inventories.map(inventoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getOwnById(String organizerEmail, UUID eventId, UUID inventoryId) {
        return inventoryMapper.toResponse(findOwnInventory(organizerEmail, eventId, inventoryId));
    }

    @Override
    @Transactional
    public InventoryResponse update(
            String organizerEmail,
            UUID eventId,
            UUID inventoryId,
            UpdateInventoryRequest request) {
        ShowTimeTicketInventory inventory = findOwnInventory(organizerEmail, eventId, inventoryId);
        requireDraft(inventory.getShowTime().getEvent());
        applyTotalQuantity(inventory, request.getTotalQuantity());
        return inventoryMapper.toResponse(inventoryRepository.save(inventory));
    }

    private void ensurePairs(List<ShowTime> showTimes, List<TicketType> ticketTypes, int defaultTotalQuantity) {
        for (ShowTime showTime : showTimes) {
            for (TicketType ticketType : ticketTypes) {
                if (inventoryRepository.existsByShowTimeIdAndTicketTypeId(showTime.getId(), ticketType.getId())) {
                    continue;
                }
                inventoryRepository.save(ShowTimeTicketInventory.builder()
                        .showTime(showTime)
                        .ticketType(ticketType)
                        .totalQuantity(defaultTotalQuantity)
                        .reservedQuantity(0)
                        .soldQuantity(0)
                        .build());
            }
        }
    }

    private void applyTotalQuantity(ShowTimeTicketInventory inventory, Integer totalQuantity) {
        int reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
        int sold = inventory.getSoldQuantity() == null ? 0 : inventory.getSoldQuantity();
        validateTotalQuantity(totalQuantity, reserved, sold);
        inventory.setTotalQuantity(totalQuantity);
        if (inventory.getReservedQuantity() == null) {
            inventory.setReservedQuantity(0);
        }
        if (inventory.getSoldQuantity() == null) {
            inventory.setSoldQuantity(0);
        }
    }

    private void validateTotalQuantity(Integer totalQuantity, int reserved, int sold) {
        if (totalQuantity == null || totalQuantity < 0) {
            throw new AppException(ErrorCode.INVALID_INVENTORY_DATA, "Tổng số lượng không được nhỏ hơn 0");
        }
        if (totalQuantity < reserved + sold) {
            throw new AppException(
                    ErrorCode.INVALID_INVENTORY_DATA,
                    "Tổng số lượng không được nhỏ hơn số vé đã giữ chỗ và đã bán");
        }
    }

    private ShowTimeTicketInventory findOwnInventory(String organizerEmail, UUID eventId, UUID inventoryId) {
        Event event = findOwnEvent(organizerEmail, eventId);
        return inventoryRepository.findByIdAndShowTimeEventId(inventoryId, event.getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
    }

    private Event findOwnDraftEvent(String organizerEmail, UUID eventId) {
        Event event = findOwnEvent(organizerEmail, eventId);
        requireDraft(event);
        return event;
    }

    private Event findOwnEvent(String organizerEmail, UUID eventId) {
        User organizer = findUser(organizerEmail);
        return eventRepository.findByIdAndOrganizerId(eventId, organizer.getId())
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
    }

    private User findUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void requireDraft(Event event) {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_EVENT_STATUS);
        }
    }
}
