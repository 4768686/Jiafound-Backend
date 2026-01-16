package org.example.campususer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置类
 * 从 application.properties 读取 jwt.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * JWT 签名密钥
     */
    private String secret;

    /**
     * JWT 过期时间（毫秒）
     * 默认：86400000ms = 24小时
     */
    private Long expiration;

    /**
     * JWT Token 请求头名称
     * 默认：Authorization
     */
    private String header = "Authorization";

    /**
     * JWT Token 前缀
     * 默认：Bearer 
     */
    private String prefix = "Bearer ";

    /**
     * 从请求头中提取 Token（去除前缀）
     */
    public String extractToken(String headerValue) {
        if (headerValue != null && headerValue.startsWith(prefix)) {
            return headerValue.substring(prefix.length());
        }
        return null;
    }

    /**
     * 构建完整的 Token 字符串（带前缀）
     */
    public String buildTokenHeader(String token) {
        return prefix + token;
    }
}
