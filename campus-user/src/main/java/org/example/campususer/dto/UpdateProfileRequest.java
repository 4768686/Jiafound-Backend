package org.example.campususer.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 更新用户信息请求 DTO
 */
@Data
public class UpdateProfileRequest {

    /**
     * 昵称（可选）
     */
    private String nickname;

    /**
     * 头像URL（可选）
     */
    private String avatarUrl;

    /**
     * 新邮箱（可选，需要验证码）
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 邮箱验证码（修改邮箱时必填）
     */
    private String verifyCode;
}
