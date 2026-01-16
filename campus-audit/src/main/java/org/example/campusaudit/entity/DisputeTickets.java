package org.example.campusaudit.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 纠纷裁决工单表
 * @TableName dispute_tickets
 */
@TableName(value ="dispute_tickets")
@Data
public class DisputeTickets {
    /**
     * 主键UUID
     */
    @TableId
    private String ticketId;

    /**
     * 关联的具体认领记录
     */
    private String claimId;

    /**
     * 发起人ID
     */
    private String initiatorId;

    /**
     * 纠纷原因
     */
    private String reason;

    /**
     * 证据列表(图片URL数组)
     */
    private Object evidenceData;

    /**
     * 状态: Reviewing, Closed, Revoked
     */
    private String status;

    /**
     * 裁决结果: OwnerWin, FinderWin...
     */
    private String rulingResult;

    /**
     * 举证截止时间
     */
    private Date deadline;

    /**
     * 
     */
    private Date createTime;

    /**
     * 
     */
    private Date updateTime;
}