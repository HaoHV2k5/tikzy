package com.tikzy.event.controller;

import com.tikzy.common.response.ApiResponse;
import com.tikzy.event.dto.request.SearchEventsRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.dto.response.PublicEventDetailResponse;
import com.tikzy.event.service.EventService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Validated
public class PublicEventController {

    private final EventService eventService;

    @GetMapping
    public ApiResponse<List<EventResponse>> getPublished(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách sự kiện thành công",
                eventService.getPublished(
                        categoryId,
                        eventPageRequest(page, size)));
    }

    @GetMapping("/search")
    public ApiResponse<List<EventResponse>> search(
            SearchEventsRequest request,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Tìm kiếm sự kiện thành công",
                eventService.searchPublished(request, eventPageRequest(page, size)));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<PublicEventDetailResponse> getPublishedById(@PathVariable UUID eventId) {
        return ApiResponse.ok(
                "Lấy thông tin sự kiện thành công",
                eventService.getPublishedById(eventId));
    }

    private PageRequest eventPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
