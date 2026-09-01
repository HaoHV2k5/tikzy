package com.tikzy.common.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brevo")
public record BrevoProperties(
        String apiKey,
        String apiUrl,
        String senderEmail,
        String senderName) {
}
