package org.example.campusaudit.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DisputeDetailDTO {
    // 1. 工单基础信息
    private String ticketId;
    private String claimId;
    private String status;
    private String reason;
    private String rulingResult;
    private LocalDateTime deadline;
    private LocalDateTime createTime;

    // 2. 身份信息 (来自 View)
    private String initiatorId;      // 发起人 ID
    private String respondentId;     // 被投诉人 ID
    private String initiatorRole;    // 发起人身份 (OWNER/CLAIMER)

    // 3. 结构化证据数据
    private EvidenceDetail initiatorEvidence; // 发起人的证据
    private EvidenceDetail respondentEvidence; // 被投诉人的证据

    @Data
    public static class EvidenceDetail {
        private List<String> imageUrls;
        private String comments;
    }
}
