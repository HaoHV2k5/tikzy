package com.tikzy.email.service.impl;

import com.tikzy.common.exception.AppException;
import com.tikzy.common.exception.ErrorCode;
import com.tikzy.email.dto.RenderedEmailTemplate;
import com.tikzy.email.entity.EmailTemplate;
import com.tikzy.email.repository.EmailTemplateRepository;
import com.tikzy.email.service.EmailService;
import com.tikzy.email.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailTemplateServiceImpl implements EmailTemplateService {

    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_.-]+)\\s*\\}\\}");

    private final EmailTemplateRepository templateRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public RenderedEmailTemplate render(String code, Map<String, ?> variables) {
        EmailTemplate template = findActiveTemplate(code);
        Map<String, ?> safeVariables = variables == null ? Map.of() : variables;

        return new RenderedEmailTemplate(
                renderContent(template.getSubject(), safeVariables, false),
                renderContent(template.getHtmlContent(), safeVariables, true),
                renderNullable(template.getTextContent(), safeVariables));
    }

    public void sendTemplate(
            String code,
            String recipientEmail,
            String recipientName,
            Map<String, ?> variables) {
        RenderedEmailTemplate rendered = render(code, variables);
        emailService.send(
                recipientEmail,
                recipientName,
                rendered.subject(),
                rendered.htmlContent(),
                rendered.textContent());
    }

    private EmailTemplate findActiveTemplate(String code) {
        if (!StringUtils.hasText(code)) {
            throw new AppException(ErrorCode.EMAIL_TEMPLATE_NOT_FOUND);
        }

        return templateRepository.findActiveByCode(code.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_TEMPLATE_NOT_FOUND));
    }

    private String renderNullable(String content, Map<String, ?> variables) {
        return content == null ? null : renderContent(content, variables, false);
    }

    private String renderContent(String content, Map<String, ?> variables, boolean html) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object variableValue = variables.get(variableName);
            if (variableValue == null) {
                throw new AppException(ErrorCode.EMAIL_TEMPLATE_VARIABLE_MISSING);
            }

            String replacement = String.valueOf(variableValue);
            if (html) {
                replacement = HtmlUtils.htmlEscape(replacement);
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }
}
