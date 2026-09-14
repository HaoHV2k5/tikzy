package com.tikzy.event.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class SearchEventsRequest {

    private String keyword;
    private String location;
    private LocalDate from;
    private LocalDate to;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
