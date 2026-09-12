package com.tikzy.event.service;

import com.tikzy.event.dto.request.CreateCategoryProposalRequest;
import com.tikzy.event.dto.request.ReviewCategoryRequest;
import com.tikzy.event.dto.response.CategoryRequestResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CategoryRequestService {

    CategoryRequestResponse create(
            String requesterEmail,
            CreateCategoryProposalRequest request);

    Page<CategoryRequestResponse> getAll(String status, Pageable pageable);

    CategoryRequestResponse review(
            UUID requestId,
            String reviewerEmail,
            ReviewCategoryRequest request);
}
