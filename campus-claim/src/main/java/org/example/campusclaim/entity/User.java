package org.example.campusclaim.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户实体类
 * 对应数据库中的 users 表
 */
@Data
@TableName("users")
public class User {
    /**
     * 用户ID (主键)
     * 对应 users.txt 中的 user_id
     */
    @TableId
    private String userId;

    /**
     * 微信 OpenID
     * 对应 open_id
     */
    private String openId;

    /**
     * 学号
     * 对应 student_id
     */
    private String studentId;

    /**
     * 真实姓名
     * 对应 real_name
     */
    private String realName;

    /**
     * 昵称
     * 对应 nickname
     */
    private String nickname;

    /**
     * 头像URL
     * 对应 avatar_url
     */
    private String avatarUrl;

    /**
     * 邮箱 (核心字段：用于联系方式)
     * 对应 email
     */
    private String email;

    /**
     * 是否认证 (0/1)
     * 对应 is_certified
     */
    private Boolean isCertified;

    /**
     * 角色类型 (User/Admin/SuperAdmin)
     * 对应 role_type
     */
    private String roleType;

    /**
     * 账号状态 (Normal/Frozen)
     * 对应 account_status
     */
    private String accountStatus;

    /**
     * 解封时间
     * 对应 unfreeze_time
     */
    private Date unfreezeTime;

    /**
     * 赏币余额
     * 对应 coin_balance
     */
    private BigDecimal coinBalance;

    /**
     * 冻结余额
     * 对应 frozen_balance
     */
    private BigDecimal frozenBalance;

    /**
     * 创建时间
     * 对应 create_time
     */
    private Date createTime;

    /**
     * 更新时间
     * 对应 update_time
     */
    private Date updateTime;
}