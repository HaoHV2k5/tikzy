package com.tikzy.event.service.impl;

import com.tikzy.auth.entity.User;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.storage.ImageStorageService;
import com.tikzy.common.storage.StoredImage;
import com.tikzy.event.dto.request.CreateEventRequest;
import com.tikzy.event.dto.request.SearchEventsRequest;
import com.tikzy.event.dto.request.UpdateEventRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.dto.response.PublicEventDetailResponse;
import com.tikzy.event.dto.response.PublicShowTimeResponse;
import com.tikzy.event.dto.response.PublicTicketOfferResponse;
import com.tikzy.event.entity.Category;
import com.tikzy.event.entity.Event;
import com.tikzy.event.entity.ShowTime;
import com.tikzy.event.entity.TicketType;
import com.tikzy.event.enums.CategoryStatus;
import com.tikzy.event.enums.EventImageType;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import com.tikzy.event.mapper.EventMapper;
import com.tikzy.event.repository.CategoryRepository;
import com.tikzy.event.repository.EventRepository;
import com.tikzy.event.repository.ShowTimeRepository;
import com.tikzy.event.repository.TicketTypeRepository;
import com.tikzy.event.service.EventService;
import com.tikzy.ticket.entity.ShowTimeTicketInventory;
import com.tikzy.ticket.repository.ShowTimeTicketInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final BigDecimal MAX_REFUND_FEE = new BigDecimal("100");
    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif");

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;
    private final ImageStorageService imageStorageService;
    private final ShowTimeRepository showTimeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final ShowTimeTicketInventoryRepository inventoryRepository;

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
        event.setRefundPolicy(refundPolicy);
        event.setRefundDeadlineDays(refundDeadlineDays);
        event.setRefundFeePercentage(refundFeePercentage);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse uploadImage(
            String organizerEmail,
            UUID eventId,
            String imageType,
            MultipartFile file) {
        Event event = findOwnEvent(organizerEmail, eventId);
        requireDraft(event);
        EventImageType type = parseImageType(imageType);
        byte[] content = readImage(file);
        StoredImage storedImage = imageStorageService.upload(content, imagePublicId(event.getId(), type));
        applyImageUrl(event, type, storedImage.url());
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse deleteImage(String organizerEmail, UUID eventId, String imageType) {
        Event event = findOwnEvent(organizerEmail, eventId);
        requireDraft(event);
        EventImageType type = parseImageType(imageType);
        if (!StringUtils.hasText(currentImageUrl(event, type))) {
            throw new AppException(ErrorCode.EVENT_IMAGE_NOT_FOUND);
        }
        imageStorageService.delete(imagePublicId(event.getId(), type));
        applyImageUrl(event, type, null);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional
    public EventResponse deleteDraft(String organizerEmail, UUID eventId) {
        Event event = findOwnEvent(organizerEmail, eventId);
        requireDraft(event);
        deleteStoredImage(event, EventImageType.BANNER);
        deleteStoredImage(event, EventImageType.THUMBNAIL);
        EventResponse response = eventMapper.toResponse(event);
        eventRepository.delete(event);
        return response;
    }

    @Override
    @Transactional
    public EventResponse publish(String organizerEmail, UUID eventId) {
        Event event = findOwnEvent(organizerEmail, eventId);
        requireDraft(event);
        requireReadyToPublish(event.getId());
        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> getPublished(UUID categoryId, Pageable pageable) {
        if (categoryId == null) {
            return eventRepository.findAllByStatus(EventStatus.PUBLISHED, pageable)
                    .map(eventMapper::toPublicResponse);
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        if (category.getStatus() != CategoryStatus.PUBLISHED) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        return eventRepository.findAllByCategoryIdAndStatus(
                        category.getId(),
                        EventStatus.PUBLISHED,
                        pageable)
                .map(eventMapper::toPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> searchPublished(SearchEventsRequest request, Pageable pageable) {
        SearchEventsRequest query = request == null ? new SearchEventsRequest() : request;
        String keyword = containsPattern(query.getKeyword());
        String location = containsPattern(query.getLocation());
        LocalDate from = query.getFrom();
        LocalDate to = query.getTo();
        BigDecimal minPrice = query.getMinPrice();
        BigDecimal maxPrice = query.getMaxPrice();
        validateSearchRange(from, to, minPrice, maxPrice);
        return eventRepository.searchPublished(
                        EventStatus.PUBLISHED,
                        keyword,
                        location,
                        from == null ? null : from.atStartOfDay(),
                        to == null ? null : to.atTime(LocalTime.MAX),
                        minPrice,
                        maxPrice,
                        pageable)
                .map(eventMapper::toPublicResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicEventDetailResponse getPublishedById(UUID eventId) {
        Event event = eventRepository.findByIdAndStatus(eventId, EventStatus.PUBLISHED)
                .orElseThrow(() -> new AppException(ErrorCode.EVENT_NOT_FOUND));
        return PublicEventDetailResponse.from(
                eventMapper.toPublicResponse(event),
                toPublicShowTimes(event.getId()));
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

    private void requireReadyToPublish(UUID eventId) {
        List<ShowTime> showTimes = showTimeRepository.findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(eventId);
        if (showTimes.isEmpty()) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Sự kiện cần ít nhất một suất diễn đang hoạt động");
        }
        List<TicketType> ticketTypes = ticketTypeRepository.findAllByEventIdAndIsActiveTrue(eventId);
        if (ticketTypes.isEmpty()) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Sự kiện cần ít nhất một hạng vé đang hoạt động");
        }
        Map<String, ShowTimeTicketInventory> inventories = inventoriesByPair(eventId);
        for (ShowTime showTime : showTimes) {
            for (TicketType ticketType : ticketTypes) {
                if (!inventories.containsKey(inventoryKey(showTime.getId(), ticketType.getId()))) {
                    throw new AppException(
                            ErrorCode.INVALID_EVENT_DATA,
                            "Sự kiện cần khởi tạo tồn kho cho mọi suất diễn và hạng vé");
                }
            }
        }
    }

    private List<PublicShowTimeResponse> toPublicShowTimes(UUID eventId) {
        List<ShowTime> showTimes = showTimeRepository.findAllByEventIdAndIsActiveTrueOrderByStartTimeAsc(eventId);
        List<TicketType> ticketTypes = ticketTypeRepository.findAllByEventIdAndIsActiveTrue(eventId);
        Map<String, ShowTimeTicketInventory> inventories = inventoriesByPair(eventId);
        List<PublicShowTimeResponse> responses = new ArrayList<>();
        for (ShowTime showTime : showTimes) {
            List<PublicTicketOfferResponse> offers = new ArrayList<>();
            for (TicketType ticketType : ticketTypes) {
                ShowTimeTicketInventory inventory =
                        inventories.get(inventoryKey(showTime.getId(), ticketType.getId()));
                if (inventory == null) {
                    continue;
                }
                offers.add(PublicTicketOfferResponse.builder()
                        .ticketTypeId(ticketType.getId())
                        .name(ticketType.getName())
                        .price(ticketType.getPrice())
                        .maxPerOrder(ticketType.getMaxPerOrder())
                        .availableQuantity(availableQuantity(inventory))
                        .build());
            }
            responses.add(PublicShowTimeResponse.builder()
                    .id(showTime.getId())
                    .startTime(showTime.getStartTime())
                    .endTime(showTime.getEndTime())
                    .ticketOffers(offers)
                    .build());
        }
        return responses;
    }

    private Map<String, ShowTimeTicketInventory> inventoriesByPair(UUID eventId) {
        return inventoryRepository.findAllByShowTimeEventId(eventId).stream()
                .collect(Collectors.toMap(
                        inventory -> inventoryKey(
                                inventory.getShowTime().getId(),
                                inventory.getTicketType().getId()),
                        Function.identity(),
                        (first, ignored) -> first));
    }

    private String inventoryKey(UUID showTimeId, UUID ticketTypeId) {
        return showTimeId + ":" + ticketTypeId;
    }

    private int availableQuantity(ShowTimeTicketInventory inventory) {
        int reserved = inventory.getReservedQuantity() == null ? 0 : inventory.getReservedQuantity();
        int sold = inventory.getSoldQuantity() == null ? 0 : inventory.getSoldQuantity();
        return Math.max(0, inventory.getTotalQuantity() - reserved - sold);
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

    private EventImageType parseImageType(String imageType) {
        if (!StringUtils.hasText(imageType)) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_IMAGE,
                    "Loại ảnh phải là banner hoặc thumbnail");
        }
        try {
            return EventImageType.valueOf(imageType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_IMAGE,
                    "Loại ảnh phải là banner hoặc thumbnail");
        }
    }

    private byte[] readImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_EVENT_IMAGE, "Thiếu file ảnh");
        }
        if (file.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new AppException(ErrorCode.INVALID_EVENT_IMAGE, "Ảnh không được vượt quá 10MB");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_IMAGE,
                    "Ảnh sự kiện chỉ nhận JPEG, PNG, WEBP hoặc GIF");
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.INVALID_EVENT_IMAGE, "Không đọc được file ảnh");
        }
    }

    private void deleteStoredImage(Event event, EventImageType type) {
        if (!StringUtils.hasText(currentImageUrl(event, type))) {
            return;
        }
        imageStorageService.delete(imagePublicId(event.getId(), type));
    }

    private String imagePublicId(UUID eventId, EventImageType type) {
        return "events/" + eventId + "/" + type.name().toLowerCase(Locale.ROOT);
    }

    private String currentImageUrl(Event event, EventImageType type) {
        return type == EventImageType.BANNER ? event.getBannerUrl() : event.getThumbnailUrl();
    }

    private void applyImageUrl(Event event, EventImageType type, String url) {
        if (type == EventImageType.BANNER) {
            event.setBannerUrl(url);
            return;
        }
        event.setThumbnailUrl(url);
    }

    private void validateSearchRange(
            LocalDate from,
            LocalDate to,
            BigDecimal minPrice,
            BigDecimal maxPrice) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Thời gian bắt đầu không được sau thời gian kết thúc");
        }
        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Mức giá tối thiểu không được âm");
        }
        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Mức giá tối đa không được âm");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new AppException(
                    ErrorCode.INVALID_EVENT_DATA,
                    "Mức giá tối thiểu không được lớn hơn mức giá tối đa");
        }
    }

    private String containsPattern(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
