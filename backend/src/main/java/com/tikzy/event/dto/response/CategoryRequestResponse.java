package com.tikzy.event.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tikzy.event.enums.CategoryRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryRequestResponse {

    private UUID id;
    private UUID requesterId;
    private String requesterEmail;
    private String proposedName;
    private String description;
    private CategoryRequestStatus status;
    private String reviewNote;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private CategoryResponse category;
    private LocalDateTime createdAt;
}
