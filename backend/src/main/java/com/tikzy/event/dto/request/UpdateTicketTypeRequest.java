package com.tikzy.event.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateTicketTypeRequest {

    @Size(min = 1, max = 100, message = "Tên hạng vé phải từ 1 đến 100 ký tự")
    private String name;

    @DecimalMin(value = "0.00", message = "Giá vé không được nhỏ hơn 0")
    @Digits(integer = 13, fraction = 2, message = "Giá vé tối đa 13 chữ số phần nguyên và 2 chữ số thập phân")
    private BigDecimal price;

    @Min(value = 1, message = "Giới hạn mỗi đơn phải lớn hơn 0")
    private Integer maxPerOrder;

    private Boolean isActive;
}
