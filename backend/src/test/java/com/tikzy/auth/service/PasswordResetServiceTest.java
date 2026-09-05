package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.ForgotPasswordRequest;
import com.tikzy.auth.dto.request.ResetPasswordRequest;
import com.tikzy.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.tikzy.auth.dto.response.PasswordResetVerificationResponse;
import com.tikzy.auth.entity.AccountUnlockRequest;
import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.enums.AccountRecoveryType;
import com.tikzy.auth.repository.AccountUnlockRequestRepository;
import com.tikzy.auth.repository.RefreshTokenRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.email.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String OTP = "123456";
    private static final String CURRENT_PASSWORD = "old-password";

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountUnlockRequestRepository accountUnlockRequestRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private EmailTemplateService emailTemplateService;
    @Mock
    private AccessTokenRevocationService accessTokenRevocationService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PasswordResetService passwordResetService;
    private User user;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository,
                accountUnlockRequestRepository,
                refreshTokenRepository,
                passwordEncoder,
                emailTemplateService,
                accessTokenRevocationService,
                10,
                10,
                5);
        user = activeUser();
    }

    @Test
    void requestReset_existingActiveUserSendsHashedOtp() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("  USER@Example.com ");
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user));

        passwordResetService.requestReset(request);

        ArgumentCaptor<AccountUnlockRequest> requestCaptor =
                ArgumentCaptor.forClass(AccountUnlockRequest.class);
        verify(accountUnlockRequestRepository).consumeActiveByUserAndRequestType(
                eq(user), eq(AccountRecoveryType.PASSWORD_RESET), any(LocalDateTime.class));
        verify(accountUnlockRequestRepository).save(requestCaptor.capture());
        AccountUnlockRequest savedRequest = requestCaptor.getValue();

        assertEquals(AccountRecoveryType.PASSWORD_RESET, savedRequest.getRequestType());
        assertEquals(0, savedRequest.getOtpAttempts());
        assertTrue(savedRequest.getOtpExpiresAt().isAfter(LocalDateTime.now()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailTemplateService).sendTemplate(
                eq("PASSWORD_RESET_OTP"), eq(EMAIL), eq("Test User"), variablesCaptor.capture());
        String sentOtp = String.valueOf(variablesCaptor.getValue().get("otp"));
        assertTrue(sentOtp.matches("\\d{6}"));
        assertTrue(passwordEncoder.matches(sentOtp, savedRequest.getOtpHash()));
    }

    @Test
    void requestReset_unknownUserDoesNotRevealAccountState() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(EMAIL);
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.empty());

        passwordResetService.requestReset(request);

        verifyNoInteractions(accountUnlockRequestRepository, emailTemplateService);
    }

    @Test
    void verifyOtp_validOtpReturnsOneTimeResetToken() {
        VerifyPasswordResetOtpRequest request = new VerifyPasswordResetOtpRequest();
        request.setEmail(EMAIL);
        request.setOtp(OTP);
        AccountUnlockRequest resetRequest = resetRequest();
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user));
        when(accountUnlockRequestRepository
                .findFirstByUserAndRequestTypeAndConsumedAtIsNullAndOtpVerifiedAtIsNullOrderByCreatedAtDesc(
                        user, AccountRecoveryType.PASSWORD_RESET))
                .thenReturn(Optional.of(resetRequest));

        PasswordResetVerificationResponse response = passwordResetService.verifyOtp(request);

        assertNotNull(response.resetToken());
        assertEquals(600, response.expiresInSeconds());
        assertNotNull(resetRequest.getOtpVerifiedAt());
        assertEquals(64, resetRequest.getResetTokenHash().length());
        assertTrue(resetRequest.getResetTokenExpiresAt().isAfter(LocalDateTime.now()));
        verify(accountUnlockRequestRepository).save(resetRequest);
    }

    @Test
    void verifyOtp_wrongOtpIsRejected() {
        VerifyPasswordResetOtpRequest request = new VerifyPasswordResetOtpRequest();
        request.setEmail(EMAIL);
        request.setOtp("000000");
        AccountUnlockRequest resetRequest = resetRequest();
        when(userRepository.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user));
        when(accountUnlockRequestRepository
                .findFirstByUserAndRequestTypeAndConsumedAtIsNullAndOtpVerifiedAtIsNullOrderByCreatedAtDesc(
                        user, AccountRecoveryType.PASSWORD_RESET))
                .thenReturn(Optional.of(resetRequest));

        AppException exception = assertThrows(
                AppException.class,
                () -> passwordResetService.verifyOtp(request));

        assertEquals(ErrorCode.INVALID_PASSWORD_RESET_OTP, exception.getErrorCode());
        assertEquals(1, resetRequest.getOtpAttempts());
        verify(accountUnlockRequestRepository, never()).save(resetRequest);
    }

    @Test
    void resetPassword_updatesPasswordAndRevokesAllSessions() throws Exception {
        String resetToken = "one-time-reset-token";
        AccountUnlockRequest resetRequest = resetRequest();
        resetRequest.setOtpVerifiedAt(LocalDateTime.now().minusMinutes(1));
        resetRequest.setResetTokenHash(hash(resetToken));
        resetRequest.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(accountUnlockRequestRepository.findByResetTokenHashAndRequestType(
                hash(resetToken), AccountRecoveryType.PASSWORD_RESET))
                .thenReturn(Optional.of(resetRequest));
        when(userRepository.findByIdForUpdate(user.getId())).thenReturn(Optional.of(user));
        when(accountUnlockRequestRepository.findByResetTokenHashAndRequestTypeForUpdate(
                hash(resetToken), AccountRecoveryType.PASSWORD_RESET))
                .thenReturn(Optional.of(resetRequest));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken(resetToken);
        request.setNewPassword("new-password");
        request.setConfirmPassword("new-password");

        passwordResetService.resetPassword(request);

        assertTrue(passwordEncoder.matches("new-password", user.getPasswordHash()));
        assertFalse(passwordEncoder.matches(CURRENT_PASSWORD, user.getPasswordHash()));
        assertEquals(0, user.getFailedLoginAttempts());
        assertNotNull(resetRequest.getConsumedAt());
        verify(accessTokenRevocationService).invalidateAll(user);
        verify(refreshTokenRepository).revokeAllActiveByUser(user);
        verify(userRepository).save(user);
        verify(accountUnlockRequestRepository).save(resetRequest);
    }

    @Test
    void resetPassword_mismatchedConfirmationIsRejectedBeforeTokenLookup() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("reset-token");
        request.setNewPassword("new-password");
        request.setConfirmPassword("different-password");

        AppException exception = assertThrows(
                AppException.class,
                () -> passwordResetService.resetPassword(request));

        assertEquals(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH, exception.getErrorCode());
        verifyNoInteractions(accountUnlockRequestRepository, userRepository);
    }

    private User activeUser() {
        Role role = Role.builder().code("ROLE_CUSTOMER").name("Khách hàng").build();
        User activeUser = User.builder()
                .role(role)
                .email(EMAIL)
                .passwordHash(passwordEncoder.encode(CURRENT_PASSWORD))
                .fullName("Test User")
                .isActive(true)
                .isLocked(false)
                .build();
        activeUser.setId(UUID.randomUUID());
        return activeUser;
    }

    private AccountUnlockRequest resetRequest() {
        return AccountUnlockRequest.builder()
                .user(user)
                .requestType(AccountRecoveryType.PASSWORD_RESET)
                .otpHash(passwordEncoder.encode(OTP))
                .otpAttempts(0)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
    }

    private String hash(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
    }
}
