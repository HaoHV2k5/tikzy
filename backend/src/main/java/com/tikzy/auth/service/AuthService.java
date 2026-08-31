package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.LoginRequest;
import com.tikzy.auth.dto.request.RegisterRequest;
import com.tikzy.auth.dto.response.AuthResponse;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.entity.RefreshToken;
import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.mapper.UserMapper;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.RefreshTokenRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.config.JwtTokenProvider;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;

@Slf4j
@Service
public class AuthService {

    private static final String DEFAULT_ROLE_CODE = "ROLE_CUSTOMER";
    private static final int REFRESH_TOKEN_BYTES = 64;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenRevocationService accessTokenRevocationService;
    private final UserMapper userMapper;
    private final long refreshTokenExpirationMs;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AccessTokenRevocationService accessTokenRevocationService,
            UserMapper userMapper,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenRevocationService = accessTokenRevocationService;
        this.userMapper = userMapper;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null;
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        Role role = roleRepository.findByCode(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED, "Role mặc định chưa được seed"));

        User user = User.builder()
                .role(role)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .isActive(true)
                .build();

        User saved = userRepository.saveAndFlush(user);
        log.info("Đăng ký tài khoản mới: {}", saved.getEmail());
        return userMapper.toUserResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .deviceInfo(normalizeForStorage(deviceInfo, 500))
                .ipAddress(normalizeForStorage(ipAddress, 45))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs)))
                .isRevoked(false)
                .build());

        log.info("Đăng nhập thành công: {}", user.getEmail());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .user(userMapper.toUserResponse(user))
                .refreshToken(refreshTokenValue)
                .build();
    }

    /**
     * Rotates a refresh token while holding a database lock on the current row.
     * AppException must not roll back revocations made during reuse detection.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse refresh(String refreshToken, String deviceInfo, String ipAddress) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken currentToken = refreshTokenRepository.findByToken(refreshToken.trim())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));
        User user = currentToken.getUser();

        if (Boolean.TRUE.equals(currentToken.getIsRevoked())) {
            revokeAllSessions(user);
            log.warn("Phát hiện refresh token bị tái sử dụng: userId={}", user.getId());
            throw new AppException(ErrorCode.REFRESH_TOKEN_REUSED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (currentToken.getExpiresAt() == null || !currentToken.getExpiresAt().isAfter(now)) {
            currentToken.setIsRevoked(true);
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            revokeAllSessions(user);
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        currentToken.setIsRevoked(true);
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rotatedTokenValue = generateRefreshToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(rotatedTokenValue)
                .deviceInfo(metadataOrPrevious(deviceInfo, currentToken.getDeviceInfo(), 500))
                .ipAddress(metadataOrPrevious(ipAddress, currentToken.getIpAddress(), 45))
                .expiresAt(now.plus(Duration.ofMillis(refreshTokenExpirationMs)))
                .isRevoked(false)
                .build());

        log.info("Refresh token đã được rotate: userId={}", user.getId());
        return AuthResponse.builder()
                .accessToken(accessToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .user(userMapper.toUserResponse(user))
                .refreshToken(rotatedTokenValue)
                .build();
    }

    /**
     * Thu hồi refresh token của thiết bị hiện tại. Logout được thiết kế
     * idempotent để client có thể gọi lại khi token đã hết hạn hoặc đã bị revoke.
     */
    @Transactional
    public void logout(String refreshToken) {
        logout(refreshToken, null);
    }

    /**
     * Thu hồi refresh token và blacklist access token hiện tại của thiết bị.
     * Access token không hợp lệ được bỏ qua để logout vẫn idempotent.
     */
    @Transactional
    public void logout(String refreshToken, String accessToken) {
        if (!StringUtils.hasText(refreshToken)) {
            blacklistAccessToken(accessToken);
            return;
        }

        refreshTokenRepository.findByToken(refreshToken.trim())
                .ifPresent(token -> token.setIsRevoked(true));
        blacklistAccessToken(accessToken);
    }

    /**
     * Thu hồi toàn bộ refresh token của user. Email từ access token được ưu tiên;
     * refresh token là fallback cho trường hợp access token đã hết hạn.
     */
    @Transactional
    public void logoutAll(String email, String refreshToken) {
        User user = findUserForLogout(email, refreshToken);
        if (user != null) {
            User lockedUser = lockUserForLogout(user);
            revokeAllSessions(lockedUser);
        }
    }

    private void revokeAllSessions(User user) {
        accessTokenRevocationService.invalidateAll(user);
        refreshTokenRepository.revokeAllActiveByUser(user);
    }

    private void blacklistAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken) || !jwtTokenProvider.validateToken(accessToken)) {
            return;
        }

        Claims claims = jwtTokenProvider.getClaims(accessToken);
        accessTokenRevocationService.blacklist(claims);
    }

    private User lockUserForLogout(User user) {
        if (user.getId() == null) {
            return user;
        }
        return userRepository.findByIdForUpdate(user.getId()).orElse(user);
    }

    private User findUserForLogout(String email, String refreshToken) {
        if (StringUtils.hasText(email)) {
            return userRepository.findByEmail(normalizeEmail(email)).orElse(null);
        }

        if (StringUtils.hasText(refreshToken)) {
            return refreshTokenRepository.findByToken(refreshToken.trim())
                    .map(RefreshToken::getUser)
                    .orElse(null);
        }

        return null;
    }

    private String generateRefreshToken() {
        byte[] tokenBytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String normalizeForStorage(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
    }

    private String metadataOrPrevious(String value, String previousValue, int maxLength) {
        String normalized = normalizeForStorage(value, maxLength);
        return normalized != null ? normalized : previousValue;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
