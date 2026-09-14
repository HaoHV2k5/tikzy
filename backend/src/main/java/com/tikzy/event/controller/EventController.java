package com.tikzy.event.controller;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.response.ApiResponse;
import com.tikzy.event.dto.request.CreateEventRequest;
import com.tikzy.event.dto.request.UpdateEventRequest;
import com.tikzy.event.dto.response.EventResponse;
import com.tikzy.event.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/events")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ORGANIZER')")
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ApiResponse<EventResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateEventRequest request) {
        return ApiResponse.ok(
                "Tạo sự kiện thành công",
                eventService.create(currentUserEmail(authentication), request));
    }

    @GetMapping
    public ApiResponse<List<EventResponse>> getOwn(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách sự kiện thành công",
                eventService.getOwn(
                        currentUserEmail(authentication),
                        status,
                        eventPageRequest(page, size)));
    }

    @GetMapping("/{eventId}")
    public ApiResponse<EventResponse> getOwnById(
            Authentication authentication,
            @PathVariable UUID eventId) {
        return ApiResponse.ok(
                "Lấy thông tin sự kiện thành công",
                eventService.getOwnById(currentUserEmail(authentication), eventId));
    }

    @PatchMapping("/{eventId}")
    public ApiResponse<EventResponse> update(
            Authentication authentication,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpdateEventRequest request) {
        return ApiResponse.ok(
                "Cập nhật sự kiện thành công",
                eventService.update(currentUserEmail(authentication), eventId, request));
    }

    @PutMapping(path = "/{eventId}/images/{imageType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EventResponse> uploadImage(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable String imageType,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(
                "Cập nhật ảnh sự kiện thành công",
                eventService.uploadImage(currentUserEmail(authentication), eventId, imageType, file));
    }

    @DeleteMapping("/{eventId}/images/{imageType}")
    public ApiResponse<EventResponse> deleteImage(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable String imageType) {
        return ApiResponse.ok(
                "Xóa ảnh sự kiện thành công",
                eventService.deleteImage(currentUserEmail(authentication), eventId, imageType));
    }

    @DeleteMapping("/{eventId}")
    public ApiResponse<EventResponse> deleteDraft(
            Authentication authentication,
            @PathVariable UUID eventId) {
        return ApiResponse.ok(
                "Xóa sự kiện thành công",
                eventService.deleteDraft(currentUserEmail(authentication), eventId));
    }

    @PostMapping("/{eventId}/publish")
    public ApiResponse<EventResponse> publish(
            Authentication authentication,
            @PathVariable UUID eventId) {
        return ApiResponse.ok(
                "Công khai sự kiện thành công",
                eventService.publish(currentUserEmail(authentication), eventId));
    }

    private String currentUserEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }

    private PageRequest eventPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
