package org.example.campusitem.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.Base64;

import org.example.campusitem.dto.openai.OpenAIResponse;

@Slf4j
@Component
public class AIImageDescriptionUtils {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    // 注意：识图也是通过 Chat Completions API 访问，URL 与文本聊天相同
    @Value("${openai.api.url}")
    private String apiUrl;

    /**
     * 根据图片文件生成物品描述
     *
     * @param imageFile 用户上传的图片文件
     * @return 物品的文字描述，如果失败返回 null
     */
    public String generateDescriptionFromImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) return null;

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageFile.getBytes());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o");
            requestBody.put("max_tokens", 300);

            // 修正：使用 List<Map<String, Object>>
            List<Map<String, Object>> messages = new ArrayList<>();
            Map<String, Object> userMessage = new HashMap<>();
            userMessage.put("role", "user");

            List<Map<String, Object>> contentParts = new ArrayList<>();

            // 文本部分
            contentParts.add(Map.of(
                    "type", "text",
                    "text", "我捡到了一个别人遗失的物品，请识别图中的物品并给出50字左右的中文描述，方便失主寻找。"
            ));

            // 图片部分
            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
            ));

            userMessage.put("content", contentParts);
            messages.add(userMessage);
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OpenAIResponse> responseEntity = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, OpenAIResponse.class);

            if (responseEntity.getBody() != null) {
                return responseEntity.getBody().getChoices().getFirst().getMessage().getContent();
            }

        } catch (Exception e) {
            log.error("识图失败: {}", e.getMessage());
            return null;
        }

        return null;
    }
}