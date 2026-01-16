package org.example.campusaudit.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Component
public class AzureBlobUtils {
    @Value("${azure.storage.sas-url}")
    private String sasUrl;

    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 生成唯一文件名
        String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

        // 2. 直接使用 SAS URL 构建 ContainerClient
        // 此时 SDK 会从 URL 中自动解析出权限信息
        BlobContainerClient containerClient = new BlobContainerClientBuilder()
                .endpoint(sasUrl)
                .buildClient();

        // 3. 获取特定的 Blob 客户端
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        // 4. 上传流，确保 2GB 服务器内存安全
        try (InputStream inputStream = file.getInputStream()) {
            blobClient.upload(inputStream, file.getSize(), true);
        }

        // 5. 返回图片的公开 URL（截断 SAS 部分，仅保留到文件名）
        String fullUrl = blobClient.getBlobUrl();
        return fullUrl.split("\\?")[0];
    }
}
