package com.paperpages.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 邮件发送器配置。
 * 始终提供一个 JavaMailSender bean（即便未配置 SMTP 也不会导致启动失败，
 * 发送动作由 MailService 的 enable 判断跳过），避免 Boot 自动配置在缺 host 时注入失败。
 * 通过环境变量 MAIL_HOST / MAIL_PORT / MAIL_USERNAME / MAIL_PASSWORD 注入。
 */
@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:465}") int port,
            @Value("${spring.mail.username:}") String username,
            @Value("${spring.mail.password:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        if (username != null && !username.isBlank()) sender.setUsername(username);
        if (password != null && !password.isBlank()) sender.setPassword(password);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        if (port == 465) {
            // SSL 隐式 TLS（QQ/163 等常用）
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            // 非 465 走 STARTTLS（587 等）
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        if (username != null && !username.isBlank()) {
            // 部分服务商要求指定发件域
            props.put("mail.smtp.from", username);
        }
        return sender;
    }
}
