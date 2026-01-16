package org.example.campusitem.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.campusitem.dto.openai.OpenAIEmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.*;
import redis.clients.jedis.search.aggr.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import java.util.*;

@Slf4j
@Component
public class EmbeddingUtils {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String apiKey;

    // 注意：Embedding 的官方端点与 Chat 不同
    @Value("${openai.embedding.url}")
    private String apiUrl;

    @Autowired
    private JedisPooled jedis;

    /**
     * 将文本转换为向量
     * @param text 输入的物品描述或搜索词
     * @return 1536 维的向量列表
     */
    public List<Double> getEmbedding(String text) {
        try {
            // 1. 构造请求头 (带安全加密的 Bearer Token)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 2. 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "text-embedding-3-small");
            requestBody.put("input", text);

            // 3. 发起请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<OpenAIEmbeddingResponse> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, OpenAIEmbeddingResponse.class);

            if (response.getBody() != null && !response.getBody().getData().isEmpty()) {
                // 返回第一个结果的向量
                return response.getBody().getData().getFirst().getEmbedding();
            }
        } catch (Exception e) {
            log.error("获取向量异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 1. 存入向量数据到 Redis Hash
     */
    public void saveVector(String itemId, String content, List<Double> embedding) {
        String key = "item:" + itemId;
        Map<String, Object> fields = new HashMap<>();
        fields.put("content", content);
        // 将向量转为 byte[] 存入
        fields.put("embedding", floatsToBytes(embedding));

        jedis.hset(key.getBytes(), fieldsToBytesMap(fields));
        log.info("已存入向量索引: {}", key);
    }

    /**
     * 2. 向量检索匹配 (KNN 搜索)
     */
    public List<String> searchSimilar(String queryText, int topK) {
        List<Double> queryEmbedding = getEmbedding(queryText);
        if (queryEmbedding == null) return Collections.emptyList();

        byte[] queryVectorBytes = floatsToBytes(queryEmbedding);

        // 构造 KNN 查询指令: *=>[KNN $topK @embedding $vec AS score]
        String queryStr = "*=>[KNN " + topK + " @embedding $vec AS score]";
        Query q = new Query(queryStr)
                .addParam("vec", queryVectorBytes)
                .setSortBy("score", true) // 按相似度距离升序排列
                .dialect(2); // 必须指定 Dialect 2 才能开启向量搜索特性

        SearchResult result = jedis.ftSearch("item_idx", q);

        List<String> itemIds = new ArrayList<>();
        result.getDocuments().forEach(doc -> {
            // doc.getId() 返回的是 "item:123"
            itemIds.add(doc.getId().replace("item:", ""));
            log.info("匹配到物品: {}, 距离评分: {}", doc.getId(), doc.get("score"));
        });

        return itemIds;
    }

    /**
     * 3. 删除给定 ID 的向量条目
     * @param itemId 物品的唯一标识
     */
    public void deleteVector(String itemId) {
        String key = "item:" + itemId;
        try {
            // 由于索引是建立在 HASH 之上的，直接删除该 Key，RediSearch 会自动同步索引
            long deletedCount = jedis.del(key);
            if (deletedCount > 0) {
                log.info("成功删除向量索引条目: {}", key);
            } else {
                log.warn("未找到待删除的向量条目: {}", key);
            }
        } catch (Exception e) {
            log.error("删除向量条目异常, itemId: {}, 错误: {}", itemId, e.getMessage());
        }
    }

    // 辅助方法：处理 hset 的二进制转换
    private Map<byte[], byte[]> fieldsToBytesMap(Map<String, Object> fields) {
        Map<byte[], byte[]> byteMap = new HashMap<>();
        fields.forEach((k, v) -> {
            if (v instanceof byte[]) {
                byteMap.put(k.getBytes(), (byte[]) v);
            } else {
                byteMap.put(k.getBytes(), String.valueOf(v).getBytes());
            }
        });
        return byteMap;
    }

    private byte[] floatsToBytes(List<Double> doubles) {
        ByteBuffer buffer = ByteBuffer.allocate(doubles.size() * 4); // float 占 4 字节
        buffer.order(ByteOrder.LITTLE_ENDIAN); // Redis 向量搜索通常使用小端序
        for (Double d : doubles) {
            buffer.putFloat(d.floatValue());
        }
        return buffer.array();
    }
}
