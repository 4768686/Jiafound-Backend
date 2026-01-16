package org.example.campusaudit.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.campusaudit.dto.openai.AIAuditResult;
import org.example.campusaudit.dto.openai.OpenAIResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Slf4j
@Component
public class AICheckUtils {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${openai.api.key}") // 在 properties 中配置
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    /**
     * 调用 AI 审核文本内容
     * @param content 待审核的用户输入
     * @return 结构化的审核结果对象
     */
    public AIAuditResult checkContent(String content) {
        try {
            // 1. 构造请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey); // 自动添加 "Bearer " 前缀

            // 2. 构造请求体 (按照 OpenAI Chat Completion 协议)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("response_format", Collections.singletonMap("type", "json_object"));

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content",
                    "你是一个校园失物招领审核员。请判断内容是否包含诈骗、广告或违规信息。只返回 JSON 格式: " +
                            "{\"suggestion\":\"PASS/BLOCK\", \"reason\":\"原因\", \"category\":\"类别\"}"));
            messages.add(Map.of("role", "user", "content", "待审内容: " + content));

            requestBody.put("messages", messages);

            // 3. 发起请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OpenAIResponse> responseEntity = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, OpenAIResponse.class);

            // 4. 解析结果
            OpenAIResponse response = responseEntity.getBody();
            if (response != null && !response.getChoices().isEmpty()) {
                // 提取 choices[0].message.content 字符串并再次解析为对象
                String jsonContent = response.getChoices().getFirst().getMessage().getContent();
                return objectMapper.readValue(jsonContent, AIAuditResult.class);
            }
        } catch (Exception e) {
            log.error("AI 审核请求发生异常: {}", e.getMessage());
        }
        // 兜底方案：发生异常时默认转人工审核
        return new AIAuditResult("REVIEW", "服务不可用，请人工核查", "SYSTEM_ERROR");
    }
}
