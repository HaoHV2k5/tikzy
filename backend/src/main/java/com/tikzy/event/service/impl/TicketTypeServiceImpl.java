package com.tikzy.event.service.impl;

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
import com.tikzy.event.mapper.TicketTypeMapper;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.TicketTypeRepository;
import com.tikzy.event.service.TicketTypeService;
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
public class TicketTypeServiceImpl implements TicketTypeService {

    private static final int DEFAULT_MAX_PER_ORDER = 10;

    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    private final TicketTypeMapper ticketTypeMapper;

    @Override
    @Transactional
    public TicketTypeResponse create(String organizerEmail, UUID eventId, CreateTicketTypeRequest request) {
        Event event = findOwnDraftEvent(organizerEmail, eventId);
        String name = cleanName(request.getName());
        rejectIfDuplicateName(event.getId(), name, null);
        validatePrice(request.getPrice());
        int maxPerOrder = request.getMaxPerOrder() == null
                ? DEFAULT_MAX_PER_ORDER
                : request.getMaxPerOrder();
        validateMaxPerOrder(maxPerOrder);

        TicketType ticketType = ticketTypeMapper.toEntity(request);
        ticketType.setEvent(event);
        ticketType.setName(name);
        ticketType.setMaxPerOrder(maxPerOrder);
        ticketType.setIsActive(true);
        return ticketTypeMapper.toResponse(ticketTypeRepository.save(ticketType));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketTypeResponse> getOwn(
            String organizerEmail,
            UUID eventId,
            Boolean active,
            Pageable pageable) {
        Event event = findOwnEvent(organizerEmail, eventId);
        Page<TicketType> ticketTypes = active == null
                ? ticketTypeRepository.findAllByEventId(event.getId(), pageable)
                : ticketTypeRepository.findAllByEventIdAndIsActive(event.getId(), active, pageable);
        return ticketTypes.map(ticketTypeMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketTypeResponse getOwnById(String organizerEmail, UUID eventId, UUID ticketTypeId) {
        return ticketTypeMapper.toResponse(findOwnTicketType(organizerEmail, eventId, ticketTypeId));
    }

    @Override
    @Transactional
    public TicketTypeResponse update(
            String organizerEmail,
            UUID eventId,
            UUID ticketTypeId,
            UpdateTicketTypeRequest request) {
        TicketType ticketType = findOwnTicketType(organizerEmail, eventId, ticketTypeId);
        requireDraft(ticketType.getEvent());

        if (request.getName() != null) {
            String name = cleanName(request.getName());
            rejectIfDuplicateName(eventId, name, ticketType.getId());
            request.setName(name);
        }
        if (request.getPrice() != null) {
            validatePrice(request.getPrice());
        }
        if (request.getMaxPerOrder() != null) {
            validateMaxPerOrder(request.getMaxPerOrder());
        }

        ticketTypeMapper.updateEntity(request, ticketType);
        return ticketTypeMapper.toResponse(ticketTypeRepository.save(ticketType));
    }

    @Override
    @Transactional
    public TicketTypeResponse deleteDraft(String organizerEmail, UUID eventId, UUID ticketTypeId) {
        TicketType ticketType = findOwnTicketType(organizerEmail, eventId, ticketTypeId);
        requireDraft(ticketType.getEvent());
        TicketTypeResponse response = ticketTypeMapper.toResponse(ticketType);
        ticketTypeRepository.delete(ticketType);
        return response;
    }

    private TicketType findOwnTicketType(String organizerEmail, UUID eventId, UUID ticketTypeId) {
        Event event = findOwnEvent(organizerEmail, eventId);
        return ticketTypeRepository.findByIdAndEventId(ticketTypeId, event.getId())
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_TYPE_NOT_FOUND));
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

    private String cleanName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new AppException(ErrorCode.INVALID_TICKET_TYPE_DATA, "Tên hạng vé là bắt buộc");
        }
        String cleaned = name.strip().replaceAll("\\s+", " ");
        if (!StringUtils.hasText(cleaned)) {
            throw new AppException(ErrorCode.INVALID_TICKET_TYPE_DATA, "Tên hạng vé là bắt buộc");
        }
        return cleaned;
    }

    private void rejectIfDuplicateName(UUID eventId, String name, UUID excludeId) {
        boolean duplicate = excludeId == null
                ? ticketTypeRepository.existsByEventIdAndNameIgnoreCase(eventId, name)
                : ticketTypeRepository.existsByEventIdAndNameIgnoreCaseAndIdNot(eventId, name, excludeId);
        if (duplicate) {
            throw new AppException(
                    ErrorCode.INVALID_TICKET_TYPE_DATA,
                    "Hạng vé đã tồn tại trong sự kiện");
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(ErrorCode.INVALID_TICKET_TYPE_DATA, "Giá vé không được nhỏ hơn 0");
        }
    }

    private void validateMaxPerOrder(Integer maxPerOrder) {
        if (maxPerOrder == null || maxPerOrder < 1) {
            throw new AppException(
                    ErrorCode.INVALID_TICKET_TYPE_DATA,
                    "Giới hạn mỗi đơn phải lớn hơn 0");
        }
    }
}
