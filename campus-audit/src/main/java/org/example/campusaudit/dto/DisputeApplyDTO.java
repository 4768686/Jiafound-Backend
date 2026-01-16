package org.example.campusaudit.dto;

import lombok.Data;
import java.util.List;

@Data
public class DisputeApplyDTO {

    private String claimId; // 32位UUID

    private String reason; // 维权理由

    private EvidenceData evidenceData; // 嵌套的证据对象

    @Data
    public static class EvidenceData {
        private List<String> imageUrls; // 图片 URL 列表
        private String comments; // 备注文字
    }
}
