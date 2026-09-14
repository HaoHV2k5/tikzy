package com.tikzy.event.service;

import com.tikzy.event.dto.request.CreateEventRequest;
import com.tikzy.event.dto.request.UpdateEventRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.dto.response.PublicEventDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface EventService {

    EventResponse create(String organizerEmail, CreateEventRequest request);

    Page<EventResponse> getOwn(String organizerEmail, String status, Pageable pageable);

    EventResponse getOwnById(String organizerEmail, UUID eventId);

    EventResponse update(String organizerEmail, UUID eventId, UpdateEventRequest request);

    EventResponse uploadImage(
            String organizerEmail,
            UUID eventId,
            String imageType,
            MultipartFile file);

    EventResponse deleteImage(String organizerEmail, UUID eventId, String imageType);

    EventResponse deleteDraft(String organizerEmail, UUID eventId);

    EventResponse publish(String organizerEmail, UUID eventId);

    Page<EventResponse> getPublished(UUID categoryId, Pageable pageable);

    PublicEventDetailResponse getPublishedById(UUID eventId);
}
