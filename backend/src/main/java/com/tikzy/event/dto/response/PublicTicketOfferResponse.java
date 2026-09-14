package com.tikzy.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicTicketOfferResponse {

    private UUID ticketTypeId;
    private String name;
    private BigDecimal price;
    private Integer maxPerOrder;
    private Integer availableQuantity;
}
