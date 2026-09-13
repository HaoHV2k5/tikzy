package com.tikzy.event.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateShowTimeRequest {

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Boolean isActive;
}
