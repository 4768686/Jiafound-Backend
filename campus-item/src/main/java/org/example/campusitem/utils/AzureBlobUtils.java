package org.example.campusitem.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

/**
 * Azure Blob Storage 工具类
 * 用于上传文件到 Azure 云存储
 */
@Component
public class AzureBlobUtils {
    
    @Value("${azure.storage.sas-url}")
    private String sasUrl;

    /**
     * 上传文件到 Azure Blob Storage
     * @param file 要上传的文件
     * @return 文件的公开访问URL
     * @throws IOException 如果上传过程中发生IO错误
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // 1. 生成带前缀的唯一文件名（avatar/前缀用于区分用户头像）
        String fileName = "avatar/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

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
