package org.example.campusclaim.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("dispute_tickets")
public class DisputeTicket {
    @TableId
    private String ticketId;
    private String claimId;
    private String initiatorId;
    private String reason;
    private String evidenceData; // 存 JSON 字符串
    private String status; // Reviewing
    private String rulingResult;
    private Date deadline;
    private Date createTime;
    private Date updateTime;
}