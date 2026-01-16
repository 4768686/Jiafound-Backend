package org.example.campususer.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 * 用于微信小程序登录
 */
@Data
public class LoginRequest {

    /**
     * 微信登录凭证 code
     */
    @NotBlank(message = "登录凭证不能为空")
    private String code;

    /**
     * 微信用户信息
     * 包含 nickName, avatarUrl 等
     */
    private Map<String, Object> userInfo;
}
