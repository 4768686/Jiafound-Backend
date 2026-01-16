package org.example.campusitem;

import lombok.extern.slf4j.Slf4j;
import org.example.campusitem.utils.EmbeddingUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.JedisPooled;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class EmbeddingUtilsTest {

    @Autowired
    private EmbeddingUtils embeddingUtils;

    @Autowired
    private JedisPooled jedis;

    @Test
    void testVectorFlow() {
        // 1. 准备测试数据
        String itemId = "test_999";
        String content = "一个蓝色的水杯，印有皮卡丘图案";

        // 2. 获取向量
        List<Double> embedding = embeddingUtils.getEmbedding(content);
        assertNotNull(embedding, "OpenAI 返回向量不应为空");
        assertEquals(1536, embedding.size(), "向量维度应为 1536");

        // 3. 存入 Redis
        embeddingUtils.saveVector(itemId, content, embedding);

        // 验证数据是否存在于 Hash 中
        assertTrue(jedis.exists("item:" + itemId), "Redis 中应存在该 Key");

        // 4. 执行语义检索测试
        // 使用相关的词进行搜索，看能否匹配到刚才存入的内容
        String queryText = "查找带卡通图案的蓝色杯子";
        List<String> results = embeddingUtils.searchSimilar(queryText, 3);

        assertFalse(results.isEmpty(), "检索结果不应为空");
        assertTrue(results.contains(itemId), "搜索结果应包含刚才存入的物品 ID");

        System.out.println("成功匹配到的 ID 列表: " + results);
    }

    @Test
    void testSearchPrecision() {
        // 测试不相关内容的干扰情况
        String irrelevantQuery = "我想吃校门口的麻辣烫";
        List<String> results = embeddingUtils.searchSimilar(irrelevantQuery, 1);

        // 即使匹配到，分值（Distance）也会非常大
        System.out.println("不相关搜索匹配结果: " + results);
    }

    @Test
    void testSimilarityComparison() {
        // 2. 执行搜索
        String queryText = "一个蓝色的有图案的杯子";
        log.info("查询关键词: [{}]", queryText);

        List<String> results = embeddingUtils.searchSimilar(queryText, 4);
        log.info("搜索结果顺序: {}", results);
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
