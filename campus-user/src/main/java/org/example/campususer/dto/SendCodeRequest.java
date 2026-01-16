package org.example.campususer.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 发送验证码请求 DTO
 */
@Data
public class SendCodeRequest {

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 验证码类型
     * bind: 绑定邮箱
     * update: 修改邮箱
     * reset: 重置密码（预留）
     */
    @NotBlank(message = "验证码类型不能为空")
    @Pattern(regexp = "^(bind|update|reset)$", message = "验证码类型不正确")
    private String type;
}
