package org.example.campususer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 邮件配置类
 * 从 application.properties 读取邮件发送者信息
 */
@Data
@Component
@ConfigurationProperties(prefix = "mail.from")
public class MailConfig {

    /**
     * 发件人名称
     */
    private String name;

    /**
     * 发件人邮箱地址
     */
    private String address;
}
