package com.tikzy.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicShowTimeResponse {

    private UUID id;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<PublicTicketOfferResponse> ticketOffers;
}
