package com.tikzy.ticket.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventoryResponse {

    private UUID id;
    private UUID eventId;
    private UUID showTimeId;
    private LocalDateTime showTimeStartTime;
    private LocalDateTime showTimeEndTime;
    private UUID ticketTypeId;
    private String ticketTypeName;
    private Integer totalQuantity;
    private Integer reservedQuantity;
    private Integer soldQuantity;
    private Integer availableQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
