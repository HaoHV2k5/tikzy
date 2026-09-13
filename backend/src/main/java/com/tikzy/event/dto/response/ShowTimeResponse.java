package com.tikzy.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShowTimeResponse {

    private UUID id;
    private UUID eventId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
