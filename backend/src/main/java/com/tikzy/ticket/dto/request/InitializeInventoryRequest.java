package com.tikzy.ticket.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitializeInventoryRequest {

    @Min(value = 0, message = "Số lượng mặc định không được nhỏ hơn 0")
    private Integer defaultTotalQuantity;
}
