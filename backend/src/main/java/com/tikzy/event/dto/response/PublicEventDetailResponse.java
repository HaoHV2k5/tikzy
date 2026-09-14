package com.tikzy.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tikzy.event.enums.EventStatus;
import com.tikzy.event.enums.RefundPolicy;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicEventDetailResponse {

    private UUID id;
    private UUID organizerId;
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
    private LocalDateTime createdAt;
    private List<PublicShowTimeResponse> showTimes;

    public static PublicEventDetailResponse from(
            EventResponse event,
            List<PublicShowTimeResponse> showTimes) {
        return PublicEventDetailResponse.builder()
                .id(event.getId())
                .organizerId(event.getOrganizerId())
                .category(event.getCategory())
                .title(event.getTitle())
                .description(event.getDescription())
                .venueName(event.getVenueName())
                .venueAddress(event.getVenueAddress())
                .bannerUrl(event.getBannerUrl())
                .thumbnailUrl(event.getThumbnailUrl())
                .status(event.getStatus())
                .refundPolicy(event.getRefundPolicy())
                .refundDeadlineDays(event.getRefundDeadlineDays())
                .refundFeePercentage(event.getRefundFeePercentage())
                .createdAt(event.getCreatedAt())
                .showTimes(showTimes)
                .build();
    }
}
