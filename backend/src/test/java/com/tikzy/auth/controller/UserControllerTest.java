package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.AdminUpdateUserRequest;
import com.tikzy.auth.dto.request.UpdateProfileRequest;
import com.tikzy.auth.dto.response.UserResponse;
import com.tikzy.auth.service.UserService;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {

    private UserService userService;
    private UserController userController;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        userController = new UserController(userService);
        authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user@example.com");
    }

    @Test
    void getMyProfile_forwardsAuthenticatedEmail() {
        UserResponse response = userResponse();
        when(userService.getMyProfile("user@example.com")).thenReturn(response);

        UserResponse actual = userController.getMyProfile(authentication).getData();

        assertEquals(response, actual);
        verify(userService).getMyProfile("user@example.com");
    }

    @Test
    void updateMyProfile_forwardsAuthenticatedEmailAndRequest() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Updated User");
        UserResponse response = userResponse();
        when(userService.updateMyProfile("user@example.com", request)).thenReturn(response);

        UserResponse actual = userController.updateMyProfile(authentication, request).getData();

        assertEquals(response, actual);
        verify(userService).updateMyProfile("user@example.com", request);
    }

    @Test
    void updateUser_forwardsUserIdAndRequest() {
        UUID userId = UUID.randomUUID();
        AdminUpdateUserRequest request = new AdminUpdateUserRequest();
        request.setFullName("Updated User");
        UserResponse response = userResponse();
        when(userService.updateUserByAdmin(userId, request)).thenReturn(response);

        UserResponse actual = userController.updateUser(userId, request).getData();

        assertEquals(response, actual);
        verify(userService).updateUserByAdmin(userId, request);
    }

    @Test
    void getMyProfile_withoutAuthentication_throwsUnauthorized() {
        Authentication unauthenticated = mock(Authentication.class);
        when(unauthenticated.isAuthenticated()).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> userController.getMyProfile(unauthenticated));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .fullName("User")
                .role("ROLE_CUSTOMER")
                .build();
    }
}
