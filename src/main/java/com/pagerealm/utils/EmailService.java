package com.pagerealm.utils;

import com.pagerealm.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

/**
 * SimpleMailMessage：
 *  - 用於發送「純文字」郵件，不能帶附件、不能發送 HTML 格式內容。適合簡單通知信。
 *
 * MimeMessageHelper：
 *  - 用於建立 MimeMessage，可發送「HTML 格式」郵件、帶附件、多媒體內容等。功能較強大，適合需要格式化內容或附檔的郵件。
 */
@Service
public class EmailService {

    private JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    //-------------------------------------

    /**
     * @description : 寄出含有Password Reset Url的信件
     * @param to : 收件者email
     * @param resetUrl : 重置密碼的網址
     */
    public void sendPasswordResetEmail(String to, String resetUrl){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("Click the link to reset your password: " + resetUrl);
        mailSender.send(message);
    }

    /**
     * @descriptiotn : 寄出含有Verification code的註冊驗證信
     * @param to
     * @param subject
     * @param text
     * @throws MessagingException
     */
    public void sendVerificationEmail(String to, String subject, String text) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        //multipart:true 開啟透過email attachment功能
        MimeMessageHelper helper = new MimeMessageHelper(message,true);

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text,true);

        mailSender.send(message);
    }



}
