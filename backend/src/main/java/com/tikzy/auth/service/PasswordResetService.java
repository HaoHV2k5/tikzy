package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.ForgotPasswordRequest;
import com.tikzy.auth.dto.request.ResetPasswordRequest;
import com.tikzy.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.tikzy.auth.dto.response.PasswordResetVerificationResponse;
import com.tikzy.auth.entity.AccountUnlockRequest;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.enums.AccountRecoveryType;
import com.tikzy.auth.repository.AccountUnlockRequestRepository;
import com.tikzy.auth.repository.RefreshTokenRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.email.service.EmailTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class PasswordResetService {

    private static final String PASSWORD_RESET_OTP_TEMPLATE = "PASSWORD_RESET_OTP";
    private static final AccountRecoveryType PASSWORD_RESET = AccountRecoveryType.PASSWORD_RESET;
    private static final int RESET_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AccountUnlockRequestRepository accountUnlockRequestRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailTemplateService emailTemplateService;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final long otpExpirationMinutes;
    private final long resetTokenExpirationMinutes;
    private final int maxOtpAttempts;

    public PasswordResetService(
            UserRepository userRepository,
            AccountUnlockRequestRepository accountUnlockRequestRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailTemplateService emailTemplateService,
            AccessTokenRevocationService accessTokenRevocationService,
            @Value("${security.account-unlock.otp-expiration-minutes:10}") long otpExpirationMinutes,
            @Value("${security.account-unlock.reset-token-expiration-minutes:10}") long resetTokenExpirationMinutes,
            @Value("${security.account-unlock.max-otp-attempts:5}") int maxOtpAttempts) {
        this.userRepository = userRepository;
        this.accountUnlockRequestRepository = accountUnlockRequestRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailTemplateService = emailTemplateService;
        this.accessTokenRevocationService = accessTokenRevocationService;
        this.otpExpirationMinutes = Math.max(1, otpExpirationMinutes);
        this.resetTokenExpirationMinutes = Math.max(1, resetTokenExpirationMinutes);
        this.maxOtpAttempts = Math.max(1, maxOtpAttempts);
    }

    @Transactional
    public void requestReset(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailForUpdate(email).orElse(null);

        // Keep the response identical for unknown, locked, and disabled accounts.
        if (user == null
                || !Boolean.TRUE.equals(user.getIsActive())
                || Boolean.TRUE.equals(user.getIsLocked())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        accountUnlockRequestRepository.consumeActiveByUserAndRequestType(user, PASSWORD_RESET, now);

        String otp = generateOtp();
        AccountUnlockRequest resetRequest = AccountUnlockRequest.builder()
                .user(user)
                .requestType(PASSWORD_RESET)
                .otpHash(passwordEncoder.encode(otp))
                .otpAttempts(0)
                .otpExpiresAt(now.plusMinutes(otpExpirationMinutes))
                .build();
        accountUnlockRequestRepository.save(resetRequest);

        try {
            emailTemplateService.sendTemplate(
                    PASSWORD_RESET_OTP_TEMPLATE,
                    user.getEmail(),
                    user.getFullName(),
                    Map.of(
                            "fullName", user.getFullName(),
                            "email", user.getEmail(),
                            "otp", otp,
                            "expiresInMinutes", otpExpirationMinutes));
        } catch (RuntimeException ex) {
            // Do not reveal whether an email belongs to a user when email delivery fails.
            log.warn("Không thể gửi OTP đặt lại mật khẩu cho {}: {}", user.getEmail(), ex.getMessage());
        }

        log.info("Đã tạo yêu cầu OTP đặt lại mật khẩu: {}", user.getEmail());
    }

    @Transactional(noRollbackFor = AppException.class)
    public PasswordResetVerificationResponse verifyOtp(VerifyPasswordResetOtpRequest request) {
        User user = findActiveUser(request.getEmail());
        AccountUnlockRequest resetRequest = accountUnlockRequestRepository
                .findFirstByUserAndRequestTypeAndConsumedAtIsNullAndOtpVerifiedAtIsNullOrderByCreatedAtDesc(
                        user, PASSWORD_RESET)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_OTP));

        LocalDateTime now = LocalDateTime.now();
        if (resetRequest.getOtpExpiresAt() == null
                || !resetRequest.getOtpExpiresAt().isAfter(now)) {
            resetRequest.setConsumedAt(now);
            throw new AppException(ErrorCode.PASSWORD_RESET_OTP_EXPIRED);
        }

        int attempts = otpAttempts(resetRequest);
        if (attempts >= maxOtpAttempts) {
            resetRequest.setConsumedAt(now);
            throw new AppException(ErrorCode.INVALID_PASSWORD_RESET_OTP);
        }

        if (!passwordEncoder.matches(request.getOtp(), resetRequest.getOtpHash())) {
            attempts++;
            resetRequest.setOtpAttempts(attempts);
            if (attempts >= maxOtpAttempts) {
                resetRequest.setConsumedAt(now);
            }
            throw new AppException(ErrorCode.INVALID_PASSWORD_RESET_OTP);
        }

        String resetToken = generateResetToken();
        resetRequest.setOtpVerifiedAt(now);
        resetRequest.setResetTokenHash(hashToken(resetToken));
        resetRequest.setResetTokenExpiresAt(now.plusMinutes(resetTokenExpirationMinutes));
        accountUnlockRequestRepository.save(resetRequest);

        return new PasswordResetVerificationResponse(
                resetToken,
                Duration.ofMinutes(resetTokenExpirationMinutes).getSeconds());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
        if (!StringUtils.hasText(request.getResetToken())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        String resetToken = request.getResetToken().trim();
        String resetTokenHash = hashToken(resetToken);
        AccountUnlockRequest tokenRequest = accountUnlockRequestRepository
                .findByResetTokenHashAndRequestType(resetTokenHash, PASSWORD_RESET)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        User user = userRepository.findByIdForUpdate(tokenRequest.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        AccountUnlockRequest resetRequest = accountUnlockRequestRepository
                .findByResetTokenHashAndRequestTypeForUpdate(resetTokenHash, PASSWORD_RESET)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        LocalDateTime now = LocalDateTime.now();
        if (resetRequest.getConsumedAt() != null
                || resetRequest.getOtpVerifiedAt() == null
                || resetRequest.getResetTokenExpiresAt() == null
                || !resetRequest.getResetTokenExpiresAt().isAfter(now)
                || !Boolean.TRUE.equals(user.getIsActive())
                || Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        accessTokenRevocationService.invalidateAll(user);
        refreshTokenRepository.revokeAllActiveByUser(user);

        resetRequest.setConsumedAt(now);
        userRepository.save(user);
        accountUnlockRequestRepository.save(resetRequest);
        log.info("Đã đặt lại mật khẩu tài khoản: {}", user.getEmail());
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmailForUpdate(normalizeEmail(email))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_OTP));
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_RESET_OTP);
        }
        return user;
    }

    private int otpAttempts(AccountUnlockRequest resetRequest) {
        return resetRequest.getOtpAttempts() == null
                ? 0
                : Math.max(0, resetRequest.getOtpAttempts());
    }

    private String generateOtp() {
        return Integer.toString(100000 + SECURE_RANDOM.nextInt(900000));
    }

    private String generateResetToken() {
        byte[] tokenBytes = new byte[RESET_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 không khả dụng", ex);
        }
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_RESET_OTP);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
