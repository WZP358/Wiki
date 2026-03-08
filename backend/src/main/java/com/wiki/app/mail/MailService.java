package com.wiki.app.mail;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MailService {
    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public boolean canSendMail(String username) {
        return StringUtils.hasText(username);
    }

    public void sendVerifyCode(String from, String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Wiki 注册验证码");
        message.setText("您的验证码是：" + code + "，5分钟内有效。");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new RuntimeException("邮件发送失败", ex);
        }
    }
}
