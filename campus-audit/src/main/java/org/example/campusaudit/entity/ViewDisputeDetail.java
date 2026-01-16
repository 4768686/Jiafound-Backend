package org.example.campusaudit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 
 * @TableName view_dispute_detail
 */
@TableName(value ="view_dispute_detail")
@Data
public class ViewDisputeDetail {
    /**
     * 主键UUID
     */
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
     * 
     */
    private String respondentId;

    /**
     * 
     */
    private String initiatorRole;

    /**
     * 主键UUID
     */
    private String itemId;
}