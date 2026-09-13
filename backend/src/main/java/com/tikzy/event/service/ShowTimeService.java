package com.tikzy.event.service;

import com.tikzy.event.dto.request.CreateShowTimeRequest;
import com.tikzy.event.dto.request.UpdateShowTimeRequest;
import com.tikzy.event.dto.response.ShowTimeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ShowTimeService {

    ShowTimeResponse create(String organizerEmail, UUID eventId, CreateShowTimeRequest request);

    Page<ShowTimeResponse> getOwn(String organizerEmail, UUID eventId, Boolean active, Pageable pageable);

    ShowTimeResponse getOwnById(String organizerEmail, UUID eventId, UUID showTimeId);

    ShowTimeResponse update(
            String organizerEmail,
            UUID eventId,
            UUID showTimeId,
            UpdateShowTimeRequest request);

    ShowTimeResponse deleteDraft(String organizerEmail, UUID eventId, UUID showTimeId);
}
