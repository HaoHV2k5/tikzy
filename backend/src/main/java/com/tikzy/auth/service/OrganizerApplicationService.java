package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.CreateOrganizerApplicationRequest;
import com.tikzy.auth.dto.request.ReviewOrganizerApplicationRequest;
import com.tikzy.auth.dto.response.OrganizerApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrganizerApplicationService {

    OrganizerApplicationResponse create(
            String applicantEmail,
            CreateOrganizerApplicationRequest request);

    OrganizerApplicationResponse getLatestForApplicant(String applicantEmail);

    Page<OrganizerApplicationResponse> getApplications(String status, Pageable pageable);

    OrganizerApplicationResponse review(
            UUID applicationId,
            String reviewerEmail,
            ReviewOrganizerApplicationRequest request);
}
