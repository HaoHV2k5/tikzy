package com.tikzy.common.email;

public interface EmailService {

    void sendHtml(String recipientEmail, String recipientName, String subject, String htmlContent);
}
