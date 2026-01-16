package org.example.campususer.dto;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 用户信息视图对象 VO
 * 用于返回给前端的用户信息（敏感信息脱敏处理）
 */
@Data
public class UserProfileVO {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 学号（脱敏处理）
     * 例如：202****234
     */
    private String studentId;

    /**
     * 邮箱（脱敏处理）
     * 例如：z***@example.com
     */
    private String email;

    /**
     * 是否已认证
     */
    private Boolean isCertified;

    /**
     * 角色类型
     */
    private String roleType;

    /**
     * 账户状态
     */
    private String accountStatus;

    /**
     * 可用赏币余额
     */
    private BigDecimal coinBalance;

    /**
     * 冻结赏币余额
     */
    private BigDecimal frozenBalance;

    /**
     * 注册时间
     */
    private String createTime;

    /**
     * 最后更新时间
     */
    private String updateTime;
}
