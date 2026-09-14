package com.tikzy.ticket.service;

import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.ticket.dto.request.InitializeInventoryRequest;
import com.tikzy.ticket.dto.request.UpdateInventoryRequest;
import com.tikzy.ticket.dto.request.UpsertInventoriesRequest;
import com.tikzy.ticket.dto.response.InventoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    void ensurePairsForShowTime(ShowTime showTime);

    void ensurePairsForTicketType(TicketType ticketType);

    void ensurePairsForEvent(Event event, int defaultTotalQuantity);

    List<InventoryResponse> initialize(String organizerEmail, UUID eventId, InitializeInventoryRequest request);

    List<InventoryResponse> upsert(String organizerEmail, UUID eventId, UpsertInventoriesRequest request);

    Page<InventoryResponse> getOwn(
            String organizerEmail,
            UUID eventId,
            UUID showTimeId,
            UUID ticketTypeId,
            Pageable pageable);

    InventoryResponse getOwnById(String organizerEmail, UUID eventId, UUID inventoryId);

    InventoryResponse update(
            String organizerEmail,
            UUID eventId,
            UUID inventoryId,
            UpdateInventoryRequest request);
}
