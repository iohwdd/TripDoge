package com.tripdog.common.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.tripdog.config.MinioConfig;
import com.tripdog.model.dto.FileUploadDTO;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件上传工具类
 * 支持本地存储和MinIO对象存储
 *
 * @author: iohw
 * @date: 2025/9/26 15:23
 * @description:
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileUploadUtils {
    private static String baseDir = "./files";
    @Value("${minio.endpoint}")
    private String MINIO_HOST;
    private final MinioConfig minioConfig;
    private final MinioUtils minioUtils;

    public static FileUploadDTO upload2Local(MultipartFile file, String path) {
        try {
            Path uploadPath = Paths.get(baseDir + path);
            if(!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            String fileType = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String fileName = new StringBuilder(UUID.randomUUID().toString()).append(fileType).toString();

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return FileUploadDTO.builder()
                .fileId(GeneratorIdUtils.getUUID())
                .fileName(fileName)
                .filePath(String.valueOf(filePath))
                .fileId(String.valueOf(System.currentTimeMillis()))
                .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 上传文件到MinIO
     *
     * @param file 要上传的文件
     * @param userId 用户ID
     * @return 上传结果DTO
     */
    public FileUploadDTO upload2Minio(MultipartFile file, Long userId, String path) {
        try {
            // 检查文件
            if (file.isEmpty()) {
                throw new RuntimeException("上传文件不能为空");
            }

            // 获取原始文件名和扩展名
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new RuntimeException("文件名无效或缺少扩展名");
            }

            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String fileName = UUID.randomUUID() + fileExtension;

            // 构建对象路径：用户ID/文件名
            String objectKey = userId + path + "/" + fileName;

            // 上传文件到MinIO
            minioUtils.putObject(objectKey, file);

            // 构建文件访问URL
            String fileUrl = minioUtils.getTemporaryUrlByPath(objectKey);

            log.info("文件上传成功: 用户ID={}, 文件名={}, 对象路径={}", userId, originalFilename, objectKey);

            return FileUploadDTO.builder()
                .fileId(GeneratorIdUtils.getUUID())
                .fileName(fileName)
                .fileUrl(fileUrl)
                .objectKey(objectKey)
                .build();

        }catch (Exception e) {
            log.error("文件上传异常: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    public String getUrlFromMinio(String minioUrl) {
        try {
            return minioUtils.getTemporaryUrlByPath(minioUrl);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据对象路径从MinIO删除文件
     *
     * @param objectKey 对象路径 (userId/fileName)
     * @param minioClient MinIO客户端
     * @param bucketName 桶名
     */
    public void deleteFromMinio(String objectKey, MinioClient minioClient, String bucketName) {
        try {
            minioUtils.removeObject(objectKey);
            log.info("文件删除成功: 对象路径={}", objectKey);
        } catch (Exception e) {
            log.error("MinIO文件删除失败: 对象路径={}, 错误={}", objectKey, e.getMessage(), e);
            throw new RuntimeException("文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除本地文件
     *
     * @param filePath 要删除的文件路径
     */
    public static void deleteLocalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("本地文件删除成功: {}", filePath);
            } else {
                log.warn("本地文件不存在，无需删除: {}", filePath);
            }
        } catch (IOException e) {
            log.error("本地文件删除失败: 文件路径={}, 错误={}", filePath, e.getMessage(), e);
            throw new RuntimeException("本地文件删除失败: " + e.getMessage());
        }
    }

    /**
     * 删除本地文件（通过File对象）
     *
     * @param file 要删除的文件对象
     */
    public static void deleteLocalFile(java.io.File file) {
        if (file != null) {
            deleteLocalFile(file.getAbsolutePath());
        }
    }
}
