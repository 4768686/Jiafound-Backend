package org.example.campususer.dto;

import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息
     */
    private UserProfileVO userInfo;

    /**
     * 是否首次登录
     */
    private Boolean isFirstLogin;

    /**
     * 是否需要实名认证
     */
    private Boolean needCertification;
}
