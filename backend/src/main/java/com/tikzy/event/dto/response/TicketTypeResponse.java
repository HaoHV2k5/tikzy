package com.tikzy.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketTypeResponse {

    private UUID id;
    private UUID eventId;
    private String name;
    private BigDecimal price;
    private Integer maxPerOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
