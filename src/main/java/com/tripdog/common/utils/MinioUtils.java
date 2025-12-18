package com.tripdog.common.utils;

import com.tripdog.common.ErrorCode;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;

@Component
@RequiredArgsConstructor
public class MinioUtils {
    @Value("${minio.bucket-name}")
    private String bucketName;

    final MinioClient minioClient;

    public MinioClient getClient() {
        return minioClient;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void putObject(String objectKey, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void putObject(String objectKey, MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 以字节形式获取对象内容，用于后端强制下载。
     */
    public byte[] getObjectBytes(String objectKey) {
        String key = normalizeObjectKey(objectKey);
        try (InputStream in = minioClient.getObject(
            GetObjectArgs.builder()
                .bucket(bucketName)
                .object(key)
                .build()
        ); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            in.transferTo(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            System.err.println("MinIO getObject failed, key=" + key + ", raw=" + objectKey + ", err=" + e.getMessage());
            throw new RuntimeException(ErrorCode.NO_FOUND_FILE.getMessage(), e);
        }
    }

    /**
     * 支持传入完整 URL，自动截取对象 key。
     */
    private String normalizeObjectKey(String key) {
        if (key == null) return null;
        // 去掉前导斜杠，避免对象名以 "/" 导致查不到
        if (key.startsWith("/")) {
            key = key.substring(1);
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            try {
                java.net.URI uri = java.net.URI.create(key);
                String path = uri.getPath(); // e.g. /bucket/obj or /obj
                if (path == null) return key;
                // 去掉开头的 "/" 和可能的 "/bucketName/"
                path = path.startsWith("/") ? path.substring(1) : path;
                if (path.startsWith(bucketName + "/")) {
                    path = path.substring(bucketName.length() + 1);
                }
                return path;
            } catch (Exception ignored) {
                return key;
            }
        }
        return key;
    }


    public String getTemporaryUrlByPath(String path) {
        String url;
        try{
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(path)
                            .expiry(60 * 60)
                            .build()
            );
        }catch (Exception e){
            throw new RuntimeException(ErrorCode.NO_FOUND_FILE.getMessage());
        }
        return url;
    }

    /**
     * 获取带有下载行为的临时 URL（Content-Disposition: attachment）
     * 浏览器访问此 URL 会直接下载文件，而不是预览
     */
    public String getDownloadUrlByPath(String path) {
        return getDownloadUrlByPath(path, null);
    }

    /**
     * 获取带有下载行为的临时 URL，支持自定义文件名
     * @param path 对象路径
     * @param fileName 下载时显示的文件名（不指定则使用原文件名）
     */
    public String getDownloadUrlByPath(String path, String fileName) {
        String objectKey = normalizeObjectKey(path);
        String dispositionFileName = fileName != null ? fileName : objectKey.substring(objectKey.lastIndexOf('/') + 1);
        String url;
        try {
            url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(60 * 60)
                            .extraQueryParams(java.util.Map.of(
                                    "response-content-disposition",
                                    "attachment; filename=\"" + dispositionFileName + "\""
                            ))
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(ErrorCode.NO_FOUND_FILE.getMessage(), e);
        }
        return url;
    }

    public void removeObject(String objectKey) {
        try {
            minioClient.removeObject(
                    io.minio.RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
