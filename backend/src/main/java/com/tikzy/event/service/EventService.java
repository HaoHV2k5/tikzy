package com.tikzy.event.service;

import com.tikzy.event.dto.request.CreateEventRequest;
import com.tikzy.event.dto.request.UpdateEventRequest;
import com.tikzy.event.dto.response.EventResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {

    EventResponse create(String organizerEmail, CreateEventRequest request);

    Page<EventResponse> getOwn(String organizerEmail, String status, Pageable pageable);

    EventResponse getOwnById(String organizerEmail, UUID eventId);

    EventResponse update(String organizerEmail, UUID eventId, UpdateEventRequest request);

    EventResponse deleteDraft(String organizerEmail, UUID eventId);
}
