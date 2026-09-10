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
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerApplicationServiceImplTest {

    @Mock
    private OrganizerApplicationRepository organizerApplicationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AccessTokenRevocationService accessTokenRevocationService;

    private final OrganizerApplicationMapper organizerApplicationMapper =
            Mappers.getMapper(OrganizerApplicationMapper.class);
    private OrganizerApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrganizerApplicationServiceImpl(
                organizerApplicationRepository,
                userRepository,
                roleRepository,
                refreshTokenRepository,
                accessTokenRevocationService,
                organizerApplicationMapper);
    }

    @Test
    void create_individualApplication_savesPendingApplication() {
        User applicant = user("customer@example.com", "ROLE_CUSTOMER");
        CreateOrganizerApplicationRequest request = individualRequest();
        when(userRepository.findByEmailForUpdate("customer@example.com"))
                .thenReturn(Optional.of(applicant));
        when(organizerApplicationRepository.saveAndFlush(any(OrganizerApplication.class)))
                .thenAnswer(invocation -> {
                    OrganizerApplication application = invocation.getArgument(0);
                    application.setId(UUID.randomUUID());
                    return application;
                });

        OrganizerApplicationResponse response = service.create(
                " Customer@Example.com ",
                request);

        assertEquals(OrganizerApplicationStatus.PENDING, response.getStatus());
        assertEquals(OrganizerType.INDIVIDUAL, response.getOrganizerType());
        assertEquals("012345678901", response.getIdentityNumber());
        assertEquals(applicant.getId(), response.getApplicantId());
        assertEquals(applicant.getEmail(), response.getApplicantEmail());
        verify(organizerApplicationRepository).saveAndFlush(any(OrganizerApplication.class));
    }

    @Test
    void create_whenPendingApplicationExists_throwsConflict() {
        User applicant = user("customer@example.com", "ROLE_CUSTOMER");
        when(userRepository.findByEmailForUpdate("customer@example.com"))
                .thenReturn(Optional.of(applicant));
        when(organizerApplicationRepository.existsByApplicantIdAndStatus(
                applicant.getId(), OrganizerApplicationStatus.PENDING))
                .thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.create("customer@example.com", individualRequest()));

        assertEquals(ErrorCode.ORGANIZER_APPLICATION_PENDING, exception.getErrorCode());
        verify(organizerApplicationRepository, never())
                .saveAndFlush(any(OrganizerApplication.class));
    }

    @Test
    void create_businessWithoutTaxCode_throwsInvalidApplication() {
        User applicant = user("customer@example.com", "ROLE_CUSTOMER");
        CreateOrganizerApplicationRequest request = individualRequest();
        request.setOrganizerType("BUSINESS");
        request.setIdentityNumber(null);
        when(userRepository.findByEmailForUpdate("customer@example.com"))
                .thenReturn(Optional.of(applicant));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.create("customer@example.com", request));

        assertEquals(ErrorCode.INVALID_ORGANIZER_APPLICATION, exception.getErrorCode());
    }

    @Test
    void review_approved_assignsOrganizerRoleAndRevokesSessions() {
        User applicant = user("customer@example.com", "ROLE_CUSTOMER");
        User reviewer = user("admin@example.com", "ROLE_ADMIN");
        OrganizerApplication application = pendingApplication(applicant);
        Role organizerRole = Role.builder()
                .code("ROLE_ORGANIZER")
                .name("Ban tổ chức")
                .build();
        ReviewOrganizerApplicationRequest request = new ReviewOrganizerApplicationRequest();
        request.setStatus("APPROVED");
        when(organizerApplicationRepository.findByIdForUpdate(application.getId()))
                .thenReturn(Optional.of(application));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(reviewer));
        when(userRepository.findByIdForUpdate(applicant.getId())).thenReturn(Optional.of(applicant));
        when(roleRepository.findByCode("ROLE_ORGANIZER")).thenReturn(Optional.of(organizerRole));
        when(organizerApplicationRepository.save(application)).thenReturn(application);

        OrganizerApplicationResponse response = service.review(
                application.getId(),
                "admin@example.com",
                request);

        assertEquals(OrganizerApplicationStatus.APPROVED, response.getStatus());
        assertEquals(reviewer.getId(), response.getReviewedBy());
        assertEquals("ROLE_ORGANIZER", applicant.getRole().getCode());
        assertNotNull(application.getReviewedAt());
        assertEquals(reviewer, application.getReviewedBy());
        verify(accessTokenRevocationService).invalidateAll(applicant);
        verify(refreshTokenRepository).revokeAllActiveByUser(applicant);
        verify(userRepository).save(applicant);
    }

    @Test
    void review_rejectedWithoutNote_throwsInvalidApplication() {
        User applicant = user("customer@example.com", "ROLE_CUSTOMER");
        OrganizerApplication application = pendingApplication(applicant);
        ReviewOrganizerApplicationRequest request = new ReviewOrganizerApplicationRequest();
        request.setStatus("REJECTED");
        when(organizerApplicationRepository.findByIdForUpdate(application.getId()))
                .thenReturn(Optional.of(application));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.review(application.getId(), "admin@example.com", request));

        assertEquals(ErrorCode.INVALID_ORGANIZER_APPLICATION, exception.getErrorCode());
        verify(userRepository, never()).findByEmail("admin@example.com");
    }

    private CreateOrganizerApplicationRequest individualRequest() {
        CreateOrganizerApplicationRequest request = new CreateOrganizerApplicationRequest();
        request.setOrganizerType("INDIVIDUAL");
        request.setOrganizerName(" Tikzy Events ");
        request.setIdentityNumber(" 012345678901 ");
        request.setContactPhone("0901234567");
        request.setAddress(" Hà Nội ");
        request.setDescription("Nhà tổ chức sự kiện");
        return request;
    }

    private OrganizerApplication pendingApplication(User applicant) {
        OrganizerApplication application = OrganizerApplication.builder()
                .applicant(applicant)
                .organizerType(OrganizerType.INDIVIDUAL)
                .organizerName("Tikzy Events")
                .identityNumber("012345678901")
                .contactPhone("0901234567")
                .address("Hà Nội")
                .status(OrganizerApplicationStatus.PENDING)
                .build();
        application.setId(UUID.randomUUID());
        return application;
    }

    private User user(String email, String roleCode) {
        User user = User.builder()
                .email(email)
                .passwordHash("hash")
                .fullName("User")
                .role(Role.builder().code(roleCode).name(roleCode).build())
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }
}
