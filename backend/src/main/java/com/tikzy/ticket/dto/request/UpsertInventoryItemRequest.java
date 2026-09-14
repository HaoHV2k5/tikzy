package com.tikzy.ticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UpsertInventoryItemRequest {

    @NotNull(message = "Suất diễn là bắt buộc")
    private UUID showTimeId;

    @NotNull(message = "Hạng vé là bắt buộc")
    private UUID ticketTypeId;

    @NotNull(message = "Tổng số lượng là bắt buộc")
    @Min(value = 0, message = "Tổng số lượng không được nhỏ hơn 0")
    private Integer totalQuantity;
}
