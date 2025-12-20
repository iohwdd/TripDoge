package com.tripdog.service.direct;

import com.tripdog.common.utils.MinioUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class CloudFileService {
    @Autowired
    private MinioUtils minioUtils;

    public void putObject(MultipartFile file, String path) {
        minioUtils.putObject(path, file);
    }

    /**
     * 获取文件临时访问 URL（预览模式）
     */
    public String getFileTmpUrl(String objectKey) {
        return minioUtils.getTemporaryUrlByPath(objectKey);
    }

    /**
     * 获取文件下载 URL（浏览器会自动下载）
     */
    public String getFileDownloadUrl(String objectKey) {
        return minioUtils.getDownloadUrlByPath(objectKey);
    }

    /**
     * 获取文件下载 URL，支持自定义下载文件名
     * @param objectKey 对象路径
     * @param fileName 下载时显示的文件名
     */
    public String getFileDownloadUrl(String objectKey, String fileName) {
        return minioUtils.getDownloadUrlByPath(objectKey, fileName);
    }
}
