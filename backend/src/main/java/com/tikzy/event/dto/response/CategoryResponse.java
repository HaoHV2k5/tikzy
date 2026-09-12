package com.tikzy.event.dto.response;

import com.tikzy.event.enums.CategoryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class CategoryResponse {

    private UUID id;
    private String name;
    private String slug;
    private String iconUrl;
    private Integer sortOrder;
    private CategoryStatus status;
    private LocalDateTime createdAt;
}
