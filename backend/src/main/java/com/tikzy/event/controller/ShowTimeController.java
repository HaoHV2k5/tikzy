package com.tikzy.event.controller;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.response.ApiResponse;
import com.tikzy.event.dto.request.CreateShowTimeRequest;
import com.tikzy.event.dto.request.UpdateShowTimeRequest;
import com.tikzy.event.dto.response.ShowTimeResponse;
import com.tikzy.event.service.ShowTimeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/events/{eventId}/show-times")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ORGANIZER')")
public class ShowTimeController {

    private final ShowTimeService showTimeService;

    @PostMapping
    public ApiResponse<ShowTimeResponse> create(
            Authentication authentication,
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateShowTimeRequest request) {
        return ApiResponse.ok(
                "Tạo suất diễn thành công",
                showTimeService.create(currentUserEmail(authentication), eventId, request));
    }

    @GetMapping
    public ApiResponse<List<ShowTimeResponse>> getOwn(
            Authentication authentication,
            @PathVariable UUID eventId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách suất diễn thành công",
                showTimeService.getOwn(
                        currentUserEmail(authentication),
                        eventId,
                        active,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "startTime"))));
    }

    @GetMapping("/{showTimeId}")
    public ApiResponse<ShowTimeResponse> getOwnById(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID showTimeId) {
        return ApiResponse.ok(
                "Lấy thông tin suất diễn thành công",
                showTimeService.getOwnById(currentUserEmail(authentication), eventId, showTimeId));
    }

    @PatchMapping("/{showTimeId}")
    public ApiResponse<ShowTimeResponse> update(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID showTimeId,
            @Valid @RequestBody UpdateShowTimeRequest request) {
        return ApiResponse.ok(
                "Cập nhật suất diễn thành công",
                showTimeService.update(currentUserEmail(authentication), eventId, showTimeId, request));
    }

    @DeleteMapping("/{showTimeId}")
    public ApiResponse<ShowTimeResponse> deleteDraft(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID showTimeId) {
        return ApiResponse.ok(
                "Xóa suất diễn thành công",
                showTimeService.deleteDraft(currentUserEmail(authentication), eventId, showTimeId));
    }

    private String currentUserEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
