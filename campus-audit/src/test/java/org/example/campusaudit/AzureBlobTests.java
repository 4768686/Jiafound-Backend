package org.example.campusaudit;

import lombok.extern.slf4j.Slf4j;
import org.example.campusaudit.utils.AzureBlobUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

@SpringBootTest
@Slf4j
class AzureBlobTests {

    @Autowired
    private AzureBlobUtils azureBlobService;

    @Test
    void testUploadLocalFile() throws Exception {
        // 1. 指定本地图片路径 (请确保该路径下确实有文件)
        File file = new File("D:/temp/gay.png");

        if (!file.exists()) {
            System.out.println("测试失败：本地图片不存在，请检查路径！");
            return;
        }

        // 2. 将本地文件封装成 MockMultipartFile
        FileInputStream input = new FileInputStream(file);

        // 3. 调用 Service 进行上传
        try (input) {
            MockMultipartFile multipartFile = new MockMultipartFile(
                    "files",                          // 参数名，对应 Controller 中的 @RequestParam
                    file.getName(),                   // 原始文件名
                    Files.probeContentType(file.toPath()), // 自动检测内容类型 (image/jpeg)
                    input                             // 文件流
            );
            System.out.println("开始上传文件: " + file.getName());
            String url = azureBlobService.uploadFile(multipartFile);

            // 4. 输出结果
            System.out.println("上传成功！");
            System.out.println("访问 URL: " + url);
        } catch (Exception e) {
            System.err.println("上传过程中发生异常: " + e.getMessage());
            log.error("上传过程中发生异常", e);
        }
    }
}
