package org.example.campusclaim.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty; 
import java.util.Date;
import lombok.Data;

/**
 * 认领申请记录表
 * @TableName claims
 */
@TableName(value ="claims")
@Data
public class Claims {
    /**
     * 主键UUID
     */
    @TableId
    private String claimId;

    /**
     * 关联物品ID
     */
    @JsonProperty("itemID") 
    private String itemId;

    /**
     * 申请人ID
     */
    @JsonProperty("applicantID") 
    private String applicantId;
    
    /**
     * 申请状态
     */
    private String status;

    /**
     * 申请理由
     */
    private String applyMessage;

    /**
     * 隐私问题答案
     */
    private String verifyAnswer;

    /**
     * 拒绝理由
     */
    private String rejectReply;

    /**
     * 拾主确认交接
     */
    private Boolean finderConfirm;

    /**
     * 失主确认交接
     */
    private Boolean ownerConfirm;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}