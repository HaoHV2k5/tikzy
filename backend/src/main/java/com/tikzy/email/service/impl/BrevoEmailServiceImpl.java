package com.tikzy.email.service.impl;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.email.config.BrevoProperties;
import com.tikzy.email.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailServiceImpl implements EmailService {

    private static final String SEND_EMAIL_PATH = "/v3/smtp/email";

    private final RestClient brevoRestClient;
    private final BrevoProperties properties;

    @Override
    public void send(
            String recipientEmail,
            String recipientName,
            String subject,
            String htmlContent,
            String textContent) {
        validateMessage(recipientEmail, subject, htmlContent, textContent);
        validateConfiguration();

        String normalizedHtmlContent = StringUtils.hasText(htmlContent) ? htmlContent : null;
        String normalizedTextContent = normalizedHtmlContent == null ? textContent : null;

        SendEmailRequest request = new SendEmailRequest(
                new Sender(properties.senderName(), properties.senderEmail()),
                List.of(new Recipient(recipientEmail.trim(), normalizeNullable(recipientName))),
                subject.trim(),
                normalizedHtmlContent,
                normalizedTextContent);

        try {
            brevoRestClient.post()
                    .uri(SEND_EMAIL_PATH)
                    .header("api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Brevo accepted transactional email for recipient={}", recipientEmail.trim());
        } catch (RestClientResponseException ex) {
            log.error("Brevo rejected transactional email: status={}", ex.getStatusCode().value());
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (RestClientException ex) {
            log.error("Brevo transactional email request failed", ex);
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private void validateMessage(
            String recipientEmail,
            String subject,
            String htmlContent,
            String textContent) {
        if (!StringUtils.hasText(recipientEmail)
                || !StringUtils.hasText(subject)
                || (!StringUtils.hasText(htmlContent) && !StringUtils.hasText(textContent))) {
            throw new AppException(ErrorCode.INVALID_EMAIL_REQUEST);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.apiKey())
                || !StringUtils.hasText(properties.senderEmail())) {
            throw new AppException(ErrorCode.EMAIL_SERVICE_NOT_CONFIGURED);
        }
    }

    private String normalizeNullable(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record SendEmailRequest(
            Sender sender,
            List<Recipient> to,
            String subject,
            String htmlContent,
            String textContent) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Sender(String name, String email) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record Recipient(String email, String name) {
    }
}
