package com.tikzy.event.dto.request;

import com.tikzy.event.enums.RefundPolicy;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class UpdateEventRequest {

    private UUID categoryId;

    @Size(min = 1, max = 500, message = "Tên sự kiện phải từ 1 đến 500 ký tự")
    private String title;

    private String description;

    @Size(max = 255, message = "Tên địa điểm không được vượt quá 255 ký tự")
    private String venueName;

    @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
    private String venueAddress;

    private RefundPolicy refundPolicy;

    @Min(value = 1, message = "Số ngày hoàn vé phải lớn hơn 0")
    private Integer refundDeadlineDays;

    @DecimalMin(value = "0.00", message = "Phí hoàn vé không được nhỏ hơn 0")
    @DecimalMax(value = "100.00", message = "Phí hoàn vé không được lớn hơn 100")
    private BigDecimal refundFeePercentage;
}
