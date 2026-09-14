package com.tikzy.event.service;

import com.tikzy.event.dto.request.CreateTicketTypeRequest;
import com.tikzy.event.dto.request.UpdateTicketTypeRequest;
import com.tikzy.event.dto.response.TicketTypeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TicketTypeService {

    TicketTypeResponse create(String organizerEmail, UUID eventId, CreateTicketTypeRequest request);

    Page<TicketTypeResponse> getOwn(String organizerEmail, UUID eventId, Boolean active, Pageable pageable);

    TicketTypeResponse getOwnById(String organizerEmail, UUID eventId, UUID ticketTypeId);

    TicketTypeResponse update(
            String organizerEmail,
            UUID eventId,
            UUID ticketTypeId,
            UpdateTicketTypeRequest request);

    TicketTypeResponse deleteDraft(String organizerEmail, UUID eventId, UUID ticketTypeId);
}
