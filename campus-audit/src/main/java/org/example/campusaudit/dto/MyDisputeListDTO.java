package org.example.campusaudit.dto;

import lombok.Data;
import java.util.Date;

@Data
public class MyDisputeListDTO {
    private String ticketId;      // 工单ID
    private String claimId;       // 认领ID
    private String status;        // 状态
    private Date createTime;      // 创建时间 (物理表字段)
    private String relationType;  // "我发起的" 或 "涉及我的"
    private String reasonSummary; // 理由摘要
    private String initiatorRole; // OWNER 或 CLAIMER
    private Date deadline;        // 处理截止时间
    private String rulingResult;  // 裁决结果
}