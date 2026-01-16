package org.example.campususer.service.impl;

import org.example.campususer.config.MailConfig;
import org.example.campususer.service.MailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 邮件发送服务实现
 * 使用 @Async 实现异步邮件发送，避免阻塞主线程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final MailConfig mailConfig;

    /**
     * 异步发送验证码邮件
     * 使用 @Async 注解，邮件发送在独立线程池中执行
     * 
     * @param email 收件人邮箱
     * @param code 验证码
     */
    @Override
    @Async
    public void sendVerifyCodeAsync(String email, String code) {
        try {
            log.info("开始异步发送验证码邮件 email={}, code={}, thread={}", 
                email, code, Thread.currentThread().getName());
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailConfig.getAddress());
            message.setTo(email);
            message.setSubject("【校园失物招领】邮箱验证码");
            message.setText(String.format(
                "您的验证码是: %s\n\n验证码有效期5分钟，请尽快使用。\n\n如非本人操作，请忽略此邮件。",
                code
            ));
            
            mailSender.send(message);
            log.info("验证码邮件发送成功 email={}, thread={}", email, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("发送邮件失败 email={}, code={}, thread={}", 
                email, code, Thread.currentThread().getName(), e);
            // 注意：异步方法中的异常不会传播到调用方
            // 这里只记录日志，不抛出异常
        }
    }
}
