package com.tikzy.email.service;

public interface EmailService {

    void send(
            String recipientEmail,
            String recipientName,
            String subject,
            String htmlContent,
            String textContent);

    default void sendHtml(String recipientEmail, String recipientName, String subject, String htmlContent) {
        send(recipientEmail, recipientName, subject, htmlContent, null);
    }
}
