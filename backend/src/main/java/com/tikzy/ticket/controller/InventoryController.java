package com.tikzy.ticket.controller;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.response.ApiResponse;
import com.tikzy.ticket.dto.request.InitializeInventoryRequest;
import com.tikzy.ticket.dto.request.UpdateInventoryRequest;
import com.tikzy.ticket.dto.request.UpsertInventoriesRequest;
import com.tikzy.ticket.dto.response.InventoryResponse;
import com.tikzy.ticket.service.InventoryService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizer/events/{eventId}/inventories")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ORGANIZER')")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/initialize")
    public ApiResponse<List<InventoryResponse>> initialize(
            Authentication authentication,
            @PathVariable UUID eventId,
            @Valid @RequestBody(required = false) InitializeInventoryRequest request) {
        return ApiResponse.ok(
                "Khởi tạo tồn kho thành công",
                inventoryService.initialize(currentUserEmail(authentication), eventId, request));
    }

    @PutMapping
    public ApiResponse<List<InventoryResponse>> upsert(
            Authentication authentication,
            @PathVariable UUID eventId,
            @Valid @RequestBody UpsertInventoriesRequest request) {
        return ApiResponse.ok(
                "Cập nhật tồn kho thành công",
                inventoryService.upsert(currentUserEmail(authentication), eventId, request));
    }

    @GetMapping
    public ApiResponse<List<InventoryResponse>> getOwn(
            Authentication authentication,
            @PathVariable UUID eventId,
            @RequestParam(required = false) UUID showTimeId,
            @RequestParam(required = false) UUID ticketTypeId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(
                "Lấy danh sách tồn kho thành công",
                inventoryService.getOwn(
                        currentUserEmail(authentication),
                        eventId,
                        showTimeId,
                        ticketTypeId,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by("showTime.startTime").ascending()
                                        .and(Sort.by("ticketType.name").ascending()))));
    }

    @GetMapping("/{inventoryId}")
    public ApiResponse<InventoryResponse> getOwnById(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID inventoryId) {
        return ApiResponse.ok(
                "Lấy thông tin tồn kho thành công",
                inventoryService.getOwnById(currentUserEmail(authentication), eventId, inventoryId));
    }

    @PatchMapping("/{inventoryId}")
    public ApiResponse<InventoryResponse> update(
            Authentication authentication,
            @PathVariable UUID eventId,
            @PathVariable UUID inventoryId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return ApiResponse.ok(
                "Cập nhật tồn kho thành công",
                inventoryService.update(currentUserEmail(authentication), eventId, inventoryId, request));
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
