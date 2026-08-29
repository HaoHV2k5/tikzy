package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.LoginRequest;
import com.tikzy.auth.dto.request.RegisterRequest;
import com.tikzy.auth.dto.response.AuthResponse;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.mapper.UserMapper;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.common.config.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "password123";

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    private AuthService authService;

    private Role customerRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder, jwtTokenProvider, userMapper);
        customerRole = Role.builder().code("ROLE_CUSTOMER").name("Khách hàng").build();
        customerRole.setId(UUID.randomUUID());
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("  NewUser@Example.com ");
        request.setPassword(RAW_PASSWORD);
        request.setFullName("  Nguyễn Văn A  ");
        request.setPhone("0912345678");
        return request;
    }

    @Test
    void register_success_normalizesAndHashesPassword() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(false);
        when(roleRepository.findByCode("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        UserResponse response = authService.register(request);

        assertEquals("newuser@example.com", response.getEmail());
        assertEquals("Nguyễn Văn A", response.getFullName());
        assertEquals("ROLE_CUSTOMER", response.getRole());
        assertNotNull(response.getCreatedAt());
    }

    @Test
    void register_success_storesBCryptHash() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(false);
        when(roleRepository.findByCode("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.register(request);

        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).saveAndFlush(captor.capture());
        String hash = captor.getValue().getPasswordHash();
        assertNotEquals(RAW_PASSWORD, hash);
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, hash));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(registerRequest()));
        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void register_duplicatePhone_throws() {
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("0912345678")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(registerRequest()));
        assertEquals(ErrorCode.PHONE_ALREADY_EXISTS, ex.getErrorCode());
    }

    private User existingUser(boolean active) {
        User user = User.builder()
                .role(customerRole)
                .email("user@example.com")
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .fullName("User")
                .isActive(active)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword(RAW_PASSWORD);
        return request;
    }

    @Test
    void login_success_returnsAccessToken() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser(true)));
        when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("jwt-token");
        lenient().when(jwtTokenProvider.getAccessTokenExpirationSeconds()).thenReturn(1800L);

        AuthResponse response = authService.login(loginRequest());

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertEquals("user@example.com", response.getUser().getEmail());
    }

    @Test
    void login_emailNotFound_throwsInvalidCredentials() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> authService.login(loginRequest()));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser(true)));
        LoginRequest request = loginRequest();
        request.setPassword("wrong-password");

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
    }

    @Test
    void login_disabledAccount_throwsAccountDisabled() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser(false)));

        AppException ex = assertThrows(AppException.class, () -> authService.login(loginRequest()));
        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
    }
}
