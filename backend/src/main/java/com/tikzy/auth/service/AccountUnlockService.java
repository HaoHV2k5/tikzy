package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.ResetAccountPasswordRequest;
import com.tikzy.auth.dto.request.UnlockAccountRequest;
import com.tikzy.auth.dto.request.VerifyAccountUnlockOtpRequest;
import com.tikzy.auth.dto.response.AccountUnlockVerificationResponse;
import com.tikzy.auth.entity.AccountUnlockRequest;
import com.tikzy.auth.entity.User;
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
public class AccountUnlockService {

    private static final String ACCOUNT_UNLOCK_OTP_TEMPLATE = "ACCOUNT_UNLOCK_OTP";
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

    public AccountUnlockService(
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
    public void requestUnlock(UnlockAccountRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmailForUpdate(email).orElse(null);

        // Keep this response indistinguishable for unknown, active, and admin-disabled accounts.
        if (user == null
                || !Boolean.TRUE.equals(user.getIsActive())
                || !Boolean.TRUE.equals(user.getIsLocked())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        accountUnlockRequestRepository.consumeActiveByUser(user, now);

        String otp = generateOtp();
        AccountUnlockRequest unlockRequest = AccountUnlockRequest.builder()
                .user(user)
                .otpHash(passwordEncoder.encode(otp))
                .otpAttempts(0)
                .otpExpiresAt(now.plusMinutes(otpExpirationMinutes))
                .build();
        accountUnlockRequestRepository.save(unlockRequest);

        emailTemplateService.sendTemplate(
                ACCOUNT_UNLOCK_OTP_TEMPLATE,
                user.getEmail(),
                user.getFullName(),
                Map.of(
                        "fullName", user.getFullName(),
                        "email", user.getEmail(),
                        "otp", otp,
                        "expiresInMinutes", otpExpirationMinutes));

        log.info("Đã gửi OTP yêu cầu mở khóa tài khoản: {}", user.getEmail());
    }

    @Transactional(noRollbackFor = AppException.class)
    public AccountUnlockVerificationResponse verifyOtp(VerifyAccountUnlockOtpRequest request) {
        User user = findLockedUser(request.getEmail());
        AccountUnlockRequest unlockRequest = accountUnlockRequestRepository
                .findFirstByUserAndConsumedAtIsNullAndOtpVerifiedAtIsNullOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACCOUNT_UNLOCK_OTP));

        LocalDateTime now = LocalDateTime.now();
        if (unlockRequest.getOtpExpiresAt() == null
                || !unlockRequest.getOtpExpiresAt().isAfter(now)) {
            unlockRequest.setConsumedAt(now);
            throw new AppException(ErrorCode.ACCOUNT_UNLOCK_OTP_EXPIRED);
        }

        int attempts = otpAttempts(unlockRequest);
        if (attempts >= maxOtpAttempts) {
            unlockRequest.setConsumedAt(now);
            throw new AppException(ErrorCode.INVALID_ACCOUNT_UNLOCK_OTP);
        }

        if (!passwordEncoder.matches(request.getOtp(), unlockRequest.getOtpHash())) {
            attempts++;
            unlockRequest.setOtpAttempts(attempts);
            if (attempts >= maxOtpAttempts) {
                unlockRequest.setConsumedAt(now);
            }
            throw new AppException(ErrorCode.INVALID_ACCOUNT_UNLOCK_OTP);
        }

        String resetToken = generateResetToken();
        unlockRequest.setOtpVerifiedAt(now);
        unlockRequest.setResetTokenHash(hashToken(resetToken));
        unlockRequest.setResetTokenExpiresAt(now.plusMinutes(resetTokenExpirationMinutes));
        accountUnlockRequestRepository.save(unlockRequest);

        return new AccountUnlockVerificationResponse(
                resetToken,
                Duration.ofMinutes(resetTokenExpirationMinutes).getSeconds());
    }

    @Transactional
    public void resetPassword(ResetAccountPasswordRequest request) {
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        String resetToken = request.getResetToken().trim();
        String resetTokenHash = hashToken(resetToken);
        AccountUnlockRequest tokenRequest = accountUnlockRequestRepository
                .findByResetTokenHash(resetTokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        User user = userRepository.findByIdForUpdate(tokenRequest.getUser().getId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));
        AccountUnlockRequest unlockRequest = accountUnlockRequestRepository
                .findByResetTokenHashForUpdate(resetTokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        LocalDateTime now = LocalDateTime.now();
        if (unlockRequest.getConsumedAt() != null
                || unlockRequest.getOtpVerifiedAt() == null
                || unlockRequest.getResetTokenExpiresAt() == null
                || !unlockRequest.getResetTokenExpiresAt().isAfter(now)
                || !Boolean.TRUE.equals(user.getIsActive())
                || !Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setIsLocked(false);
        user.setLockedAt(null);
        accessTokenRevocationService.invalidateAll(user);
        refreshTokenRepository.revokeAllActiveByUser(user);

        unlockRequest.setConsumedAt(now);
        userRepository.save(user);
        accountUnlockRequestRepository.save(unlockRequest);
        log.info("Đã mở khóa và đặt lại mật khẩu tài khoản: {}", user.getEmail());
    }

    private User findLockedUser(String email) {
        User user = userRepository.findByEmailForUpdate(normalizeEmail(email))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_ACCOUNT_UNLOCK_OTP));
        if (!Boolean.TRUE.equals(user.getIsActive()) || !Boolean.TRUE.equals(user.getIsLocked())) {
            throw new AppException(ErrorCode.INVALID_ACCOUNT_UNLOCK_OTP);
        }
        return user;
    }

    private int otpAttempts(AccountUnlockRequest unlockRequest) {
        return unlockRequest.getOtpAttempts() == null
                ? 0
                : Math.max(0, unlockRequest.getOtpAttempts());
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
            throw new AppException(ErrorCode.INVALID_ACCOUNT_UNLOCK_OTP);
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
