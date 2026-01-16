package org.example.campususer.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 用户基础信息表
 * @TableName users
 */
@TableName(value ="users")
@Data
public class Users {
    /**
     * 主键UUID
     */
    @TableId
    private String userId;

    /**
     * 微信OpenID
     */
    private String openId;

    /**
     * 学号(加密存储)
     */
    private String studentId;

    /**
     * 真实姓名(加密存储)
     */
    private String realName;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 头像URL
     */
    private String avatarUrl;

    /**
     * 邮箱(加密存储)
     */
    private String email;

    /**
     * 是否认证: 0-否, 1-是
     */
    private Integer isCertified;

    /**
     * 角色: User, Admin, SuperAdmin
     */
    private String roleType;

    /**
     * 状态: Normal, Frozen
     */
    private String accountStatus;

    /**
     * 解封时间
     */
    private Date unfreezeTime;

    /**
     * 可用赏币
     */
    private BigDecimal coinBalance;

    /**
     * 冻结赏币
     */
    private BigDecimal frozenBalance;

    /**
     * 注册时间
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}