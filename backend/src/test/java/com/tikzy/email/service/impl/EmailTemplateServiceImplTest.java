package com.tikzy.email.service.impl;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.email.dto.RenderedEmailTemplate;
import com.tikzy.email.entity.EmailTemplate;
import com.tikzy.email.repository.EmailTemplateRepository;
import com.tikzy.email.service.EmailService;
import com.tikzy.email.service.EmailTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceImplTest {

    @Mock
    private EmailTemplateRepository templateRepository;
    @Mock
    private EmailService emailService;

    private EmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new EmailTemplateServiceImpl(templateRepository, emailService);
    }

    @Test
    void render_loadsTemplateByNormalizedCodeAndEscapesHtmlVariables() {
        EmailTemplate template = template(
                "PASSWORD_RESET_OTP",
                "Mã OTP {{otp}}",
                "<p>Xin chào {{ fullName }}</p><p>{{otp}}</p>",
                "Xin chào {{fullName}} - {{otp}}");
        when(templateRepository.findActiveByCode("PASSWORD_RESET_OTP"))
                .thenReturn(Optional.of(template));

        RenderedEmailTemplate rendered = templateService.render(
                " password_reset_otp ",
                Map.of("fullName", "User <Admin>", "otp", 123456));

        assertEquals("Mã OTP 123456", rendered.subject());
        assertEquals("<p>Xin chào User &lt;Admin&gt;</p><p>123456</p>", rendered.htmlContent());
        assertEquals("Xin chào User <Admin> - 123456", rendered.textContent());
        verify(templateRepository).findActiveByCode("PASSWORD_RESET_OTP");
    }

    @Test
    void sendTemplate_rendersAndDelegatesToEmailService() {
        EmailTemplate template = template(
                "ACCOUNT_CREATED",
                "Chào mừng {{fullName}}",
                "<p>{{email}}</p>",
                "Tài khoản {{email}}");
        when(templateRepository.findActiveByCode("ACCOUNT_CREATED"))
                .thenReturn(Optional.of(template));

        templateService.sendTemplate(
                "ACCOUNT_CREATED",
                "user@example.com",
                "User",
                Map.of("fullName", "User", "email", "user@example.com"));

        verify(emailService).send(
                "user@example.com",
                "User",
                "Chào mừng User",
                "<p>user@example.com</p>",
                "Tài khoản user@example.com");
    }

    @Test
    void render_missingVariable_throwsAndDoesNotSend() {
        EmailTemplate template = template(
                "PASSWORD_RESET_OTP",
                "OTP {{otp}}",
                "<p>{{otp}}</p>",
                null);
        when(templateRepository.findActiveByCode("PASSWORD_RESET_OTP"))
                .thenReturn(Optional.of(template));

        AppException exception = assertThrows(
                AppException.class,
                () -> templateService.sendTemplate(
                        "PASSWORD_RESET_OTP",
                        "user@example.com",
                        "User",
                        Map.of()));

        assertEquals(ErrorCode.EMAIL_TEMPLATE_VARIABLE_MISSING, exception.getErrorCode());
        verifyNoInteractions(emailService);
    }

    @Test
    void render_unknownTemplate_throws() {
        when(templateRepository.findActiveByCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> templateService.render("unknown", Map.of()));

        assertEquals(ErrorCode.EMAIL_TEMPLATE_NOT_FOUND, exception.getErrorCode());
    }

    private EmailTemplate template(String code, String subject, String htmlContent, String textContent) {
        return EmailTemplate.builder()
                .code(code)
                .name(code)
                .subject(subject)
                .htmlContent(htmlContent)
                .textContent(textContent)
                .isActive(true)
                .build();
    }
}
