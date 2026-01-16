package org.example.campusaudit.dto.openai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 审核结果的业务对象
 * 对应 OpenAI 返回 content 中的 JSON 结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIAuditResult {
    /**
     * 审核建议：PASS (通过), BLOCK (违规拦截), REVIEW (需要人工复审)
     */
    private String suggestion;

    /**
     * 判定理由
     */
    private String reason;

    /**
     * 违规分类（如：广告、诈骗、色情、暴力等）
     */
    private String category;
}
