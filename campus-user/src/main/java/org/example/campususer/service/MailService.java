package org.example.campususer.service;

/**
 * 邮件发送服务接口
 * 用于发送各类系统邮件（验证码、通知等）
 */
public interface MailService {
    
    /**
     * 异步发送验证码邮件
     * @param email 收件人邮箱
     * @param code 验证码
     */
    void sendVerifyCodeAsync(String email, String code);
}
