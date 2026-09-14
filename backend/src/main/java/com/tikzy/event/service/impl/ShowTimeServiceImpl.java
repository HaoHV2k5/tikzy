package com.tikzy.event.service.impl;

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
import com.tikzy.event.mapper.ShowTimeMapper;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.ShowTimeRepository;
import com.tikzy.event.service.ShowTimeService;
import com.tikzy.ticket.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowTimeServiceImpl implements ShowTimeService {

    private final EventRepository eventRepository;
    private final ShowTimeRepository showTimeRepository;
    private final UserRepository userRepository;
    private final ShowTimeMapper showTimeMapper;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public ShowTimeResponse create(String organizerEmail, UUID eventId, CreateShowTimeRequest request) {
        Event event = findOwnDraftEvent(organizerEmail, eventId);
        validateTimeRange(request.getStartTime(), request.getEndTime());
        rejectIfOverlapping(event.getId(), request.getStartTime(), request.getEndTime(), null);

        ShowTime showTime = showTimeMapper.toEntity(request);
        showTime.setEvent(event);
        showTime.setIsActive(true);
        ShowTime saved = showTimeRepository.save(showTime);
        inventoryService.ensurePairsForShowTime(saved);
        return showTimeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowTimeResponse> getOwn(
            String organizerEmail,
            UUID eventId,
            Boolean active,
            Pageable pageable) {
        Event event = findOwnEvent(organizerEmail, eventId);
        Page<ShowTime> showTimes = active == null
                ? showTimeRepository.findAllByEventId(event.getId(), pageable)
                : showTimeRepository.findAllByEventIdAndIsActive(event.getId(), active, pageable);
        return showTimes.map(showTimeMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ShowTimeResponse getOwnById(String organizerEmail, UUID eventId, UUID showTimeId) {
        return showTimeMapper.toResponse(findOwnShowTime(organizerEmail, eventId, showTimeId));
    }

    @Override
    @Transactional
    public ShowTimeResponse update(
            String organizerEmail,
            UUID eventId,
            UUID showTimeId,
            UpdateShowTimeRequest request) {
        ShowTime showTime = findOwnShowTime(organizerEmail, eventId, showTimeId);
        requireDraft(showTime.getEvent());

        LocalDateTime startTime = request.getStartTime() != null
                ? request.getStartTime()
                : showTime.getStartTime();
        LocalDateTime endTime = request.getEndTime() != null
                ? request.getEndTime()
                : showTime.getEndTime();
        boolean willBeActive = request.getIsActive() != null
                ? request.getIsActive()
                : Boolean.TRUE.equals(showTime.getIsActive());

        validateTimeRange(startTime, endTime);
        if (willBeActive) {
            rejectIfOverlapping(eventId, startTime, endTime, showTime.getId());
        }

        showTimeMapper.updateEntity(request, showTime);
        return showTimeMapper.toResponse(showTimeRepository.save(showTime));
    }

    @Override
    @Transactional
    public ShowTimeResponse deleteDraft(String organizerEmail, UUID eventId, UUID showTimeId) {
        ShowTime showTime = findOwnShowTime(organizerEmail, eventId, showTimeId);
        requireDraft(showTime.getEvent());
        ShowTimeResponse response = showTimeMapper.toResponse(showTime);
        showTimeRepository.delete(showTime);
        return response;
    }

    private ShowTime findOwnShowTime(String organizerEmail, UUID eventId, UUID showTimeId) {
        Event event = findOwnEvent(organizerEmail, eventId);
        return showTimeRepository.findByIdAndEventId(showTimeId, event.getId())
                .orElseThrow(() -> new AppException(ErrorCode.SHOW_TIME_NOT_FOUND));
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

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new AppException(
                    ErrorCode.INVALID_SHOW_TIME_DATA,
                    "Thời gian kết thúc phải sau thời gian bắt đầu");
        }
    }

    private void rejectIfOverlapping(
            UUID eventId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            UUID excludeId) {
        boolean overlapping = excludeId == null
                ? showTimeRepository.existsOverlapping(eventId, startTime, endTime)
                : showTimeRepository.existsOverlappingExcludingId(eventId, startTime, endTime, excludeId);
        if (overlapping) {
            throw new AppException(
                    ErrorCode.INVALID_SHOW_TIME_DATA,
                    "Suất diễn bị trùng thời gian với suất khác của sự kiện");
        }
    }
}
