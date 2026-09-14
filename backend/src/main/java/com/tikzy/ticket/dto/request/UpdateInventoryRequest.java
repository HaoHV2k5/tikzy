package com.tikzy.ticket.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInventoryRequest {

    @NotNull(message = "Tổng số lượng là bắt buộc")
    @Min(value = 0, message = "Tổng số lượng không được nhỏ hơn 0")
    private Integer totalQuantity;
}
