package org.example.campususer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置类
 * 从 application.properties 读取 wechat.* 配置项
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WeChatConfig {

    /**
     * 微信小程序 AppID
     */
    private String appid;

    /**
     * 微信小程序 AppSecret
     */
    private String secret;

    /**
     * 授权类型（默认为 authorization_code）
     */
    private String grantType = "authorization_code";

    /**
     * 微信 API 基础地址
     */
    private static final String API_BASE_URL = "https://api.weixin.qq.com";

    /**
     * 获取 code2Session 接口地址
     */
    public String getCode2SessionUrl(String code) {
        return String.format(
            "%s/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=%s",
            API_BASE_URL, appid, secret, code, grantType
        );
    }

    /**
     * 获取 Access Token 接口地址
     */
    public String getAccessTokenUrl() {
        return String.format(
            "%s/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
            API_BASE_URL, appid, secret
        );
    }
}
