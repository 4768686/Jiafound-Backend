package org.example.campusaudit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.example.campusaudit.utils.AICheckUtils;
import org.example.campusaudit.dto.openai.AIAuditResult;

@SpringBootTest
class CampusAuditApplicationTests {
    @Autowired
    AICheckUtils aiCheckUtils;

    @Test
    void contextLoads() {
    }

    @Test
    void testAICheck() {
        String text = "操场捡到一个白色钱包，里面有现金和身份证，联系不上失主。";

        AIAuditResult result = aiCheckUtils.checkContent(text);
        System.out.println("AI 审核结果: " + result.getReason() + result.getSuggestion());
    }
}
