package com.tikzy.auth.service.impl;

import com.tikzy.auth.dto.request.CreateOrganizerApplicationRequest;
import com.tikzy.auth.dto.request.ReviewOrganizerApplicationRequest;
import com.tikzy.auth.dto.response.OrganizerApplicationResponse;
import com.tikzy.auth.entity.OrganizerApplication;
import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.enums.OrganizerApplicationStatus;
import com.tikzy.auth.enums.OrganizerType;
import com.tikzy.auth.mapper.OrganizerApplicationMapper;
import com.tikzy.auth.repository.OrganizerApplicationRepository;
import com.tikzy.auth.repository.RefreshTokenRepository;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.auth.service.AccessTokenRevocationService;
import com.tikzy.auth.service.OrganizerApplicationService;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizerApplicationServiceImpl implements OrganizerApplicationService {

    private static final String CUSTOMER_ROLE = "ROLE_CUSTOMER";
    private static final String ORGANIZER_ROLE = "ROLE_ORGANIZER";
    private static final EnumSet<OrganizerApplicationStatus> ACTIVE_DOCUMENT_STATUSES =
            EnumSet.of(OrganizerApplicationStatus.PENDING, OrganizerApplicationStatus.APPROVED);

    private final OrganizerApplicationRepository organizerApplicationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final OrganizerApplicationMapper organizerApplicationMapper;

    @Override
    @Transactional
    public OrganizerApplicationResponse create(
            String applicantEmail,
            CreateOrganizerApplicationRequest request) {
        User applicant = findUserForUpdate(applicantEmail);
        String currentRole = applicant.getRole() == null ? null : applicant.getRole().getCode();
        if (ORGANIZER_ROLE.equals(currentRole)) {
            throw new AppException(ErrorCode.ALREADY_ORGANIZER);
        }
        if (!CUSTOMER_ROLE.equals(currentRole)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (organizerApplicationRepository.existsByApplicantIdAndStatus(
                applicant.getId(), OrganizerApplicationStatus.PENDING)) {
            throw new AppException(ErrorCode.ORGANIZER_APPLICATION_PENDING);
        }

        OrganizerType organizerType = OrganizerType.valueOf(request.getOrganizerType());
        String identityNumber = normalizeNullable(request.getIdentityNumber());
        String taxCode = normalizeNullable(request.getTaxCode());
        validateTypeSpecificFields(organizerType, identityNumber, taxCode);
        validateDocumentAvailability(identityNumber, taxCode);

        OrganizerApplication application = OrganizerApplication.builder()
                .applicant(applicant)
                .organizerType(organizerType)
                .organizerName(normalizeRequired(request.getOrganizerName()))
                .identityNumber(identityNumber)
                .taxCode(taxCode)
                .contactPhone(normalizeRequired(request.getContactPhone()))
                .address(normalizeRequired(request.getAddress()))
                .websiteUrl(normalizeNullable(request.getWebsiteUrl()))
                .description(normalizeNullable(request.getDescription()))
                .status(OrganizerApplicationStatus.PENDING)
                .build();

        try {
            return organizerApplicationMapper.toResponse(
                    organizerApplicationRepository.saveAndFlush(application));
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(ErrorCode.ORGANIZER_DOCUMENT_IN_USE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizerApplicationResponse getLatestForApplicant(String applicantEmail) {
        User applicant = findUser(applicantEmail);
        OrganizerApplication application = organizerApplicationRepository
                .findFirstByApplicantIdOrderByCreatedAtDesc(applicant.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ORGANIZER_APPLICATION_NOT_FOUND));
        return organizerApplicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizerApplicationResponse> getApplications(
            String status,
            Pageable pageable) {
        Page<OrganizerApplication> applications;
        if (StringUtils.hasText(status)) {
            OrganizerApplicationStatus parsedStatus;
            try {
                parsedStatus = OrganizerApplicationStatus.valueOf(
                        status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new AppException(ErrorCode.INVALID_ORGANIZER_APPLICATION);
            }
            applications = organizerApplicationRepository.findAllByStatus(parsedStatus, pageable);
        } else {
            applications = organizerApplicationRepository.findAll(pageable);
        }
        return applications.map(organizerApplicationMapper::toResponse);
    }

    @Override
    @Transactional
    public OrganizerApplicationResponse review(
            UUID applicationId,
            String reviewerEmail,
            ReviewOrganizerApplicationRequest request) {
        OrganizerApplication application = organizerApplicationRepository
                .findByIdForUpdate(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.ORGANIZER_APPLICATION_NOT_FOUND));
        if (application.getStatus() != OrganizerApplicationStatus.PENDING) {
            throw new AppException(ErrorCode.ORGANIZER_APPLICATION_ALREADY_REVIEWED);
        }

        OrganizerApplicationStatus result = OrganizerApplicationStatus.valueOf(request.getStatus());
        String reviewNote = normalizeNullable(request.getReviewNote());
        if (result == OrganizerApplicationStatus.REJECTED && reviewNote == null) {
            throw new AppException(ErrorCode.INVALID_ORGANIZER_APPLICATION);
        }

        User reviewer = findUser(reviewerEmail);
        if (result == OrganizerApplicationStatus.APPROVED) {
            approveApplicant(application.getApplicant().getId());
        }

        application.setStatus(result);
        application.setReviewNote(reviewNote);
        application.setReviewedBy(reviewer);
        application.setReviewedAt(LocalDateTime.now());
        return organizerApplicationMapper.toResponse(
                organizerApplicationRepository.save(application));
    }

    private void approveApplicant(UUID applicantId) {
        User applicant = userRepository.findByIdForUpdate(applicantId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String currentRole = applicant.getRole() == null ? null : applicant.getRole().getCode();
        if (ORGANIZER_ROLE.equals(currentRole)) {
            throw new AppException(ErrorCode.ALREADY_ORGANIZER);
        }
        if (!CUSTOMER_ROLE.equals(currentRole)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Role organizerRole = roleRepository.findByCode(ORGANIZER_ROLE)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        applicant.setRole(organizerRole);
        accessTokenRevocationService.invalidateAll(applicant);
        refreshTokenRepository.revokeAllActiveByUser(applicant);
        userRepository.save(applicant);
    }

    private void validateTypeSpecificFields(
            OrganizerType organizerType,
            String identityNumber,
            String taxCode) {
        if (organizerType == OrganizerType.INDIVIDUAL && identityNumber == null) {
            throw new AppException(ErrorCode.INVALID_ORGANIZER_APPLICATION);
        }
        if (organizerType == OrganizerType.BUSINESS && taxCode == null) {
            throw new AppException(ErrorCode.INVALID_ORGANIZER_APPLICATION);
        }
    }

    private void validateDocumentAvailability(String identityNumber, String taxCode) {
        if (identityNumber != null
                && organizerApplicationRepository.existsByIdentityNumberAndStatusIn(
                        identityNumber, ACTIVE_DOCUMENT_STATUSES)) {
            throw new AppException(ErrorCode.ORGANIZER_DOCUMENT_IN_USE);
        }
        if (taxCode != null
                && organizerApplicationRepository.existsByTaxCodeAndStatusIn(
                        taxCode, ACTIVE_DOCUMENT_STATUSES)) {
            throw new AppException(ErrorCode.ORGANIZER_DOCUMENT_IN_USE);
        }
    }

    private User findUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUserForUpdate(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        return userRepository.findByEmailForUpdate(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String normalizeRequired(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AppException(ErrorCode.INVALID_ORGANIZER_APPLICATION);
        }
        return value.trim();
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
