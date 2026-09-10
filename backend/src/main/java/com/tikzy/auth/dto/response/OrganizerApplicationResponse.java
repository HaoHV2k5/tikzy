package com.tikzy.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tikzy.auth.enums.OrganizerApplicationStatus;
import com.tikzy.auth.enums.OrganizerType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrganizerApplicationResponse {

    private UUID id;
    private UUID applicantId;
    private String applicantEmail;
    private OrganizerType organizerType;
    private String organizerName;
    private String identityNumber;
    private String taxCode;
    private String contactPhone;
    private String address;
    private String websiteUrl;
    private String description;
    private OrganizerApplicationStatus status;
    private String reviewNote;
    private UUID reviewedBy;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
