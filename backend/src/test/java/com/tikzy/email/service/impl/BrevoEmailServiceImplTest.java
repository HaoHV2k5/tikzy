package com.tikzy.email.service.impl;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.email.config.BrevoProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class BrevoEmailServiceImplTest {

    private static final String BREVO_URL = "https://api.brevo.com";

    @Test
    void sendHtml_postsTransactionalEmailToBrevo() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BREVO_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        BrevoEmailServiceImpl emailService = new BrevoEmailServiceImpl(
                restClient,
                new BrevoProperties("test-api-key", BREVO_URL, "no-reply@tikzy.test", "Tikzy"));
        server.expect(requestTo(BREVO_URL + "/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.email").value("no-reply@tikzy.test"))
                .andExpect(jsonPath("$.sender.name").value("Tikzy"))
                .andExpect(jsonPath("$.to[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.to[0].name").value("User"))
                .andExpect(jsonPath("$.subject").value("Welcome"))
                .andExpect(jsonPath("$.htmlContent").value("<p>Hello</p>"))
                .andRespond(withStatus(HttpStatus.CREATED));

        emailService.sendHtml(" user@example.com ", " User ", " Welcome ", "<p>Hello</p>");

        server.verify();
    }

    @Test
    void sendHtml_withoutBrevoConfiguration_throws() {
        RestClient restClient = RestClient.builder().baseUrl(BREVO_URL).build();
        BrevoEmailServiceImpl emailService = new BrevoEmailServiceImpl(
                restClient,
                new BrevoProperties("", BREVO_URL, "", "Tikzy"));

        AppException exception = assertThrows(
                AppException.class,
                () -> emailService.sendHtml("user@example.com", null, "Subject", "<p>Body</p>"));

        assertEquals(ErrorCode.EMAIL_SERVICE_NOT_CONFIGURED, exception.getErrorCode());
    }

    @Test
    void sendHtml_brevoFailure_throwsSafeApplicationException() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BREVO_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();
        BrevoEmailServiceImpl emailService = new BrevoEmailServiceImpl(
                restClient,
                new BrevoProperties("test-api-key", BREVO_URL, "no-reply@tikzy.test", "Tikzy"));
        server.expect(requestTo(BREVO_URL + "/v3/smtp/email"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        AppException exception = assertThrows(
                AppException.class,
                () -> emailService.sendHtml("user@example.com", null, "Subject", "<p>Body</p>"));

        assertEquals(ErrorCode.EMAIL_SEND_FAILED, exception.getErrorCode());
        server.verify();
    }
}
