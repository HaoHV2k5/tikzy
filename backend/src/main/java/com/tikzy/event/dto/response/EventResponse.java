package com.tikzy.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventResponse {

    private UUID id;
    private UUID organizerId;
    private String organizerEmail;
    private CategoryResponse category;
    private String title;
    private String description;
    private String venueName;
    private String venueAddress;
    private String bannerUrl;
    private String thumbnailUrl;
    private EventStatus status;
    private RefundPolicy refundPolicy;
    private Integer refundDeadlineDays;
    private BigDecimal refundFeePercentage;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;
}
