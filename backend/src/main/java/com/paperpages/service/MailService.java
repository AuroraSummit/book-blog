package com.paperpages.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 评论回复邮件通知。
 * 未配置 MAIL_HOST 时自动禁用（不影响评论主体功能），配置后异步发送、失败仅记日志。
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String host;
    private final String from;
    private final String siteUrl;

    public MailService(JavaMailSender mailSender,
                       @Value("${spring.mail.host:}") String host,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${paper.site-url:http://localhost:5173}") String siteUrl) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = (username == null || username.isBlank()) ? "no-reply@localhost" : username;
        this.siteUrl = (siteUrl == null || siteUrl.isBlank()) ? "http://localhost:5173"
                : (siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl);
    }

    /** 是否已配置 SMTP。 */
    public boolean isEnabled() {
        return host != null && !host.isBlank();
    }

    /** 评论收到回复时，通知被回复人的邮箱（异步，失败不影响主流程）。 */
    public void sendReplyNotification(String to, String articleTitle, String articleSlug,
                                      String parentAuthor, String replyAuthor, String replyContent) {
        if (!isEnabled() || to == null || to.isBlank()) return;
        String url = siteUrl + "/article/" + articleSlug;
        String subject = "【纸页之间】" + esc(replyAuthor) + " 回复了你在《" + esc(articleTitle) + "》下的评论";
        String html = "<html><body style=\"font-family:sans-serif;line-height:1.7;color:#333\">"
                + "<p>你好，<strong>" + esc(parentAuthor) + "</strong>：</p>"
                + "<p>" + esc(replyAuthor) + " 回复了你在《<a href=\"" + url + "\">" + esc(articleTitle) + "</a>》下的评论：</p>"
                + "<blockquote style=\"border-left:3px solid #b9a58a;margin:10px 0;padding:6px 14px;color:#666;background:#faf6ef\">"
                + esc(replyContent) + "</blockquote>"
                + "<p><a href=\"" + url + "\">前往查看 →</a></p>"
                + "<p style=\"color:#aaa;font-size:12px\">此邮件由「纸页之间」自动发送；如无需通知，忽略即可。</p>"
                + "</body></html>";

        CompletableFuture.runAsync(() -> {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
                helper.setFrom(from);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(msg);
                log.info("reply notification sent to {}", to);
            } catch (Exception e) {
                log.warn("send reply notification failed to {}: {}", to, e.getMessage());
            }
        });
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
