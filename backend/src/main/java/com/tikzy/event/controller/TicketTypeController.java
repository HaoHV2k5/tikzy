package com.tikzy.event.controller;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.response.ApiResponse;
import com.tikzy.event.dto.request.CreateTicketTypeRequest;
import com.tikzy.event.dto.request.UpdateTicketTypeRequest;
import com.tikzy.event.dto.response.TicketTypeResponse;
import com.tikzy.event.service.TicketTypeService;
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
@RequestMapping("/api/v1/organizer/events/{eventId}/ticket-types")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ORGANIZER')")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    @PostMapping
    public ApiResponse<TicketTypeResponse> create(
            Authentication authentication,
            @PathVariable UUID eventId,
            @Valid @RequestBody CreateTicketTypeRequest request) {
        return ApiResponse.ok(
                "Tạo hạng vé thành công",
                ticketTypeService.create(currentUserEmail(authentication), eventId, request));
    }

    @GetMapping
    public ApiResponse<List<TicketTypeResponse>> getOwn(
            Authentication authentication,
            @PathVariable UUID eventId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách hạng vé thành công",
                ticketTypeService.getOwn(
                        currentUserEmail(authentication),
                        eventId,
                        active,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))));
    }

    @GetMapping("/{ticketTypeId}")
    public ApiResponse<TicketTypeResponse> getOwnById(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID ticketTypeId) {
        return ApiResponse.ok(
                "Lấy thông tin hạng vé thành công",
                ticketTypeService.getOwnById(currentUserEmail(authentication), eventId, ticketTypeId));
    }

    @PatchMapping("/{ticketTypeId}")
    public ApiResponse<TicketTypeResponse> update(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID ticketTypeId,
            @Valid @RequestBody UpdateTicketTypeRequest request) {
        return ApiResponse.ok(
                "Cập nhật hạng vé thành công",
                ticketTypeService.update(currentUserEmail(authentication), eventId, ticketTypeId, request));
    }

    @DeleteMapping("/{ticketTypeId}")
    public ApiResponse<TicketTypeResponse> deleteDraft(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID ticketTypeId) {
        return ApiResponse.ok(
                "Xóa hạng vé thành công",
                ticketTypeService.deleteDraft(currentUserEmail(authentication), eventId, ticketTypeId));
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
