package org.example.campususer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 实名认证绑定请求 DTO
 */
@Data
public class BindRequest {

    /**
     * 学号
     */
    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^[0-9]{7}$", message = "学号格式不正确，应为7位数字")
    private String studentID;

    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,10}$", message = "姓名格式不正确，应为2-10个汉字")
    private String realName;

    /**
     * 邮箱地址
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 邮箱验证码
     */
    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^[0-9]{6}$", message = "验证码格式不正确，应为6位数字")
    private String verifyCode;
}
