package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.event.dto.request.CreateEventRequest;
import com.tikzy.event.dto.request.UpdateEventRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.Event;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.mapper.EventMapper;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final BigDecimal MAX_REFUND_FEE = new BigDecimal("100");

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    @Override
    @Transactional
    public EventResponse create(String organizerEmail, CreateEventRequest request) {
        User organizer = findUser(organizerEmail);
        Category category = findPublishedCategory(request.getCategoryId());
        RefundPolicy refundPolicy = request.getRefundPolicy() == null
                ? RefundPolicy.NO_REFUND
                : request.getRefundPolicy();
        validateRefundPolicy(
                refundPolicy,
                request.getRefundDeadlineDays(),
                request.getRefundFeePercentage());

        Event event = eventMapper.toEntity(request);
        event.setOrganizer(organizer);
        event.setCategory(category);
        event.setTitle(request.getTitle().trim());
        event.setDescription(normalizeNullable(request.getDescription()));
        event.setVenueName(normalizeNullable(request.getVenueName()));
        event.setVenueAddress(normalizeNullable(request.getVenueAddress()));
        event.setBannerUrl(normalizeNullable(request.getBannerUrl()));
        event.setThumbnailUrl(normalizeNullable(request.getThumbnailUrl()));
        event.setStatus(EventStatus.DRAFT);
        event.setRefundPolicy(refundPolicy);
        if (refundPolicy == RefundPolicy.NO_REFUND) {
            event.setRefundDeadlineDays(null);
            event.setRefundFeePercentage(null);
        }
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getOwn(String organizerEmail, String status, Pageable pageable) {
        User organizer = findUser(organizerEmail);
        Page<Event> events = StringUtils.hasText(status)
                ? eventRepository.findAllByOrganizerIdAndStatus(
                        organizer.getId(),
                        parseStatus(status),
                        pageable)
                : eventRepository.findAllByOrganizerId(organizer.getId(), pageable);
        return events.map(eventMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getOwnById(String organizerEmail, UUID eventId) {
        return eventMapper.toResponse(findOwnEvent(organizerEmail, eventId));
    }

    @Override
    @Transactional
    public EventResponse update(String organizerEmail, UUID eventId, UpdateEventRequest request) {
        Event event = findOwnEvent(organizerEmail, eventId);
        requireDraft(event);

        if (request.getTitle() != null && !StringUtils.hasText(request.getTitle())) {
            throw new AppException(ErrorCode.INVALID_EVENT_DATA);
        }
        if (request.getCategoryId() != null) {
            event.setCategory(findPublishedCategory(request.getCategoryId()));
        }

        RefundPolicy refundPolicy = request.getRefundPolicy() != null
                ? request.getRefundPolicy()
                : event.getRefundPolicy();
        Integer refundDeadlineDays = request.getRefundDeadlineDays() != null
                ? request.getRefundDeadlineDays()
                : event.getRefundDeadlineDays();
        BigDecimal refundFeePercentage = request.getRefundFeePercentage() != null
                ? request.getRefundFeePercentage()
                : event.getRefundFeePercentage();
        if (refundPolicy == RefundPolicy.NO_REFUND) {
            refundDeadlineDays = null;
            refundFeePercentage = null;
        }
        validateRefundPolicy(refundPolicy, refundDeadlineDays, refundFeePercentage);

        eventMapper.updateEntity(request, event);
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            event.setDescription(normalizeNullable(request.getDescription()));
        }
        if (request.getVenueName() != null) {
            event.setVenueName(normalizeNullable(request.getVenueName()));
        }
        if (request.getVenueAddress() != null) {
            event.setVenueAddress(normalizeNullable(request.getVenueAddress()));
        }
        if (request.getBannerUrl() != null) {
            event.setBannerUrl(normalizeNullable(request.getBannerUrl()));
        }
        if (request.getThumbnailUrl() != null) {
            event.setThumbnailUrl(normalizeNullable(request.getThumbnailUrl()));
        }
        event.setRefundPolicy(refundPolicy);
        event.setRefundDeadlineDays(refundDeadlineDays);
        event.setRefundFeePercentage(refundFeePercentage);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse deleteDraft(String organizerEmail, UUID eventId) {
        Event event = findOwnEvent(organizerEmail, eventId);
        requireDraft(event);
        EventResponse response = eventMapper.toResponse(event);
        eventRepository.delete(event);
        return response;
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

    private Category findPublishedCategory(UUID categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        if (category.getStatus() != CategoryStatus.PUBLISHED) {
            throw new AppException(
                    ErrorCode.INVALID_CATEGORY_STATUS,
                    "Chỉ được gắn danh mục đang công khai");
        }
        return category;
    }

    private void requireDraft(Event event) {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_EVENT_STATUS);
        }
    }

    private void validateRefundPolicy(
            RefundPolicy refundPolicy,
            Integer refundDeadlineDays,
            BigDecimal refundFeePercentage) {
        if (refundPolicy == RefundPolicy.NO_REFUND) {
            if (refundDeadlineDays != null || refundFeePercentage != null) {
                throw new AppException(
                        ErrorCode.INVALID_EVENT_DATA,
                        "Sự kiện không hoàn vé không được có hạn hoàn hoặc phí hoàn");
            }
            return;
        }
        if (refundDeadlineDays == null || refundDeadlineDays < 1) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Chính sách hoàn vé cần số ngày trước suất diễn còn được hoàn");
        }
        if (refundFeePercentage == null
                || refundFeePercentage.compareTo(BigDecimal.ZERO) < 0
                || refundFeePercentage.compareTo(MAX_REFUND_FEE) > 0) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Phí hoàn vé phải từ 0 đến 100");
        }
    }

    private EventStatus parseStatus(String status) {
        try {
            return EventStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Trạng thái sự kiện không hợp lệ");
        }
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
