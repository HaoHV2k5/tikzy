package com.tikzy.auth.service;

import com.tikzy.auth.dto.request.AdminUpdateUserRequest;
import com.tikzy.auth.dto.request.UpdateProfileRequest;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.entity.Role;
import com.tikzy.auth.entity.User;
import com.tikzy.auth.mapper.UserMapper;
import com.tikzy.auth.repository.RefreshTokenRepository;
import com.tikzy.auth.repository.RoleRepository;
import com.tikzy.auth.repository.UserRepository;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AccessTokenRevocationService accessTokenRevocationService;

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository,
                roleRepository,
                refreshTokenRepository,
                accessTokenRevocationService,
                userMapper);
    }

    @Test
    void getMyProfile_returnsMappedUser() {
        User user = existingUser();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getMyProfile(" User@Example.com ");

        assertEquals(user.getId(), response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("ROLE_CUSTOMER", response.getRole());
        verify(userRepository).findByEmail("user@example.com");
    }

    @Test
    void getMyProfile_missingUser_throws() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> userService.getMyProfile("missing@example.com"));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateMyProfile_updatesEditableFieldsAndNormalizesValues() {
        User user = existingUser();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("  Updated User  ");
        request.setPhone("0912345678");
        request.setAvatarUrl("  https://cdn.example.com/avatar.png  ");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0912345678", user.getId())).thenReturn(false);
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateMyProfile("user@example.com", request);

        assertEquals("Updated User", user.getFullName());
        assertEquals("0912345678", user.getPhone());
        assertEquals("https://cdn.example.com/avatar.png", user.getAvatarUrl());
        assertEquals("Updated User", response.getFullName());
        verify(userRepository).save(user);
    }

    @Test
    void updateMyProfile_blankPhone_clearsPhone() {
        User user = existingUser();
        user.setPhone("0911111111");
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("   ");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateMyProfile("user@example.com", request);

        assertNull(user.getPhone());
        verify(userRepository, never()).existsByPhoneAndIdNot(any(String.class), any(UUID.class));
    }

    @Test
    void updateMyProfile_duplicatePhone_throws() {
        User user = existingUser();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setPhone("0912345678");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0912345678", user.getId())).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> userService.updateMyProfile("user@example.com", request));

        assertEquals(ErrorCode.PHONE_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(user);
    }

    @Test
    void updateUserByAdmin_updatesProfileRoleAndStatusAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        User user = existingUser();
        user.setId(userId);
        Role organizerRole = Role.builder()
                .code("ROLE_ORGANIZER")
                .name("Ban tổ chức")
                .build();
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("  Updated User  ");
        request.setPhone("0912345678");
        request.setAvatarUrl(" https://cdn.example.com/new.png ");
        request.setRole("ROLE_ORGANIZER");
        request.setIsActive(false);
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0912345678", userId)).thenReturn(false);
        when(roleRepository.findByCode("ROLE_ORGANIZER")).thenReturn(Optional.of(organizerRole));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateUserByAdmin(userId, request);

        assertEquals("user@example.com", user.getEmail());
        assertEquals("Updated User", user.getFullName());
        assertEquals("0912345678", user.getPhone());
        assertEquals("https://cdn.example.com/new.png", user.getAvatarUrl());
        assertEquals("ROLE_ORGANIZER", user.getRole().getCode());
        assertEquals(false, user.getIsActive());
        assertEquals("ROLE_ORGANIZER", response.getRole());
        assertEquals(false, response.getIsActive());
        verify(accessTokenRevocationService).invalidateAll(user);
        verify(refreshTokenRepository).revokeAllActiveByUser(user);
        verify(userRepository).save(user);
    }

    @Test
    void updateUserByAdmin_duplicatePhone_throwsWithoutSaving() {
        UUID userId = UUID.randomUUID();
        User user = existingUser();
        user.setId(userId);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setPhone("0912345678");
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("0912345678", userId)).thenReturn(true);

        AppException exception = assertThrows(
                AppException.class,
                () -> userService.updateUserByAdmin(userId, request));

        assertEquals(ErrorCode.PHONE_ALREADY_EXISTS, exception.getErrorCode());
        verify(userRepository, never()).save(user);
    }

    @Test
    void updateUserByAdmin_missingUser_throws() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> userService.updateUserByAdmin(userId, new AdminUpdateUserRequest()));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateUserByAdmin_profileOnly_doesNotRevokeSessions() {
        UUID userId = UUID.randomUUID();
        User user = existingUser();
        user.setId(userId);
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("Updated User");
        when(userRepository.findByIdForUpdate(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.updateUserByAdmin(userId, request);

        verify(accessTokenRevocationService, never()).invalidateAll(user);
        verify(refreshTokenRepository, never()).revokeAllActiveByUser(user);
    }

    private User existingUser() {
        User user = User.builder()
                .role(Role.builder().code("ROLE_CUSTOMER").name("Khách hàng").build())
                .email("user@example.com")
                .phone("0900000000")
                .passwordHash("hash")
                .fullName("User")
                .avatarUrl("https://cdn.example.com/old-avatar.png")
                .isActive(true)
                .build();
        user.setId(UUID.randomUUID());
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
