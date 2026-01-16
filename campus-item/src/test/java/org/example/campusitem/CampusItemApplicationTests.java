package org.example.campusitem;

import org.example.campusitem.utils.AIImageDescriptionUtils;
import org.example.campusitem.utils.EmbeddingUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.example.campusitem.mapper.ItemsMapper;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static org.springframework.test.util.AssertionErrors.assertNotNull;

@SpringBootTest
class CampusItemApplicationTests {

    @Autowired
    private EmbeddingUtils embeddingUtils;

    @Test
    void contextLoads() {
        System.out.println("Spring 容器启动成功，配置已加载！");
    }

    @Autowired
    private ItemsMapper itemsMapper;

    @Test
    void testMapper() {
        // 执行一个简单的 count 查询，验证 SQL 是否能跑通
        Long count = itemsMapper.selectCount(null);
        System.out.println("数据库中 item 表的总记录数：" + count);
    }

    @Test
    void testEmbedding() {
        String text = "在图书馆捡到一个黑色钱包，里面有现金和身份证，联系不上失主。";
         List<Double> vector = embeddingUtils.getEmbedding(text);
         System.out.println("文本向量表示: " + vector);
    }

    @Autowired
    private AIImageDescriptionUtils imageDescriptionUtils;

    @Test
    void testGenerateDescriptionWithLocalImage() throws Exception {
        // 1. 指定本地图片路径
        File file = new File("src/main/resources/test-image.png");
        FileInputStream input = new FileInputStream(file);

        // 2. 构造 MockMultipartFile
        // 参数依次为：前端参数名, 文件原始名, 内容类型, 文件输入流
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "image",
                file.getName(),
                "image/jpeg",
                input
        );

        // 3. 调用工具类方法
        String description = imageDescriptionUtils.generateDescriptionFromImage(mockMultipartFile);

        // 4. 验证结果
        System.out.println("AI 生成的物品描述: " + description);
        assertNotNull(description, "描述不应为空");
    }

}
