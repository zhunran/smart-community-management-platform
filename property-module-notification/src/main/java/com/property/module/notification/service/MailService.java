package com.property.module.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

/**
 * 邮件发送服务
 * 支持通过 Thymeleaf 模板发送 HTML 邮件。
 * 调用方只需提供收件人、主题、模板名称和模板参数。
 * 仅在配置了 spring.mail.host 时注册，避免未配置邮件的应用（如 admin-api）启动失败。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String from;

    /**
     * 发送 HTML 模板邮件
     *
     * @param to         收件人邮箱
     * @param subject    邮件主题
     * @param template   Thymeleaf 模板路径（如 "mail/overdue-notice"）
     * @param variables  模板变量
     * @return true-发送成功 false-发送失败
     */
    public boolean sendHtml(String to, String subject, String template, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);

            // 渲染 Thymeleaf 模板
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(template, context);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("邮件发送成功 [to={}, subject={}]", to, subject);
            return true;
        } catch (MessagingException e) {
            log.error("邮件发送失败 [to={}, subject={}]", to, subject, e);
            return false;
        }
    }
}
