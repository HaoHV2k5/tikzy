package com.tikzy.email.dto;

public record RenderedEmailTemplate(
        String subject,
        String htmlContent,
        String textContent) {
}
