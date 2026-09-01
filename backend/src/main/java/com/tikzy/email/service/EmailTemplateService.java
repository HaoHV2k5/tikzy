package com.tikzy.email.service;

import com.tikzy.email.dto.RenderedEmailTemplate;

import java.util.Map;

public interface EmailTemplateService {

    RenderedEmailTemplate render(String code, Map<String, ?> variables);

    void sendTemplate(
            String code,
            String recipientEmail,
            String recipientName,
            Map<String, ?> variables);
}
