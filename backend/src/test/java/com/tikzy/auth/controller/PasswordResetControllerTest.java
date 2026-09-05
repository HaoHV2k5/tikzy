package com.tikzy.auth.controller;

import com.tikzy.auth.dto.request.ForgotPasswordRequest;
import com.tikzy.auth.dto.request.ResetPasswordRequest;
import com.tikzy.auth.dto.request.VerifyPasswordResetOtpRequest;
import com.tikzy.auth.dto.response.PasswordResetVerificationResponse;
import com.tikzy.auth.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetControllerTest {

    private PasswordResetService passwordResetService;
    private PasswordResetController passwordResetController;

    @BeforeEach
    void setUp() {
        passwordResetService = mock(PasswordResetService.class);
        passwordResetController = new PasswordResetController(passwordResetService);
    }

    @Test
    void requestPasswordReset_forwardsRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();

        passwordResetController.requestPasswordReset(request);

        verify(passwordResetService).requestReset(request);
    }

    @Test
    void verifyPasswordResetOtp_returnsVerificationToken() {
        VerifyPasswordResetOtpRequest request = new VerifyPasswordResetOtpRequest();
        PasswordResetVerificationResponse expected =
                new PasswordResetVerificationResponse("reset-token", 600);
        when(passwordResetService.verifyOtp(request)).thenReturn(expected);

        PasswordResetVerificationResponse actual =
                passwordResetController.verifyPasswordResetOtp(request).getData();

        assertEquals(expected, actual);
        verify(passwordResetService).verifyOtp(request);
    }

    @Test
    void resetPassword_forwardsRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();

        passwordResetController.resetPassword(request);

        verify(passwordResetService).resetPassword(request);
    }
}
