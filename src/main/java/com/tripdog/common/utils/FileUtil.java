package com.tripdog.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileUtil {
    public static boolean isImage(String fileName) {
        return fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".png");
    }

    public static String getFileSuffix(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
    }

    public static String getAttachmentPathPrefix(String conversationId, Long userId) {
        return "conversation/" + conversationId + "/attachment/" + userId;
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

    public static boolean isTextFile(String fileName) {
        return fileName.toLowerCase().endsWith(".txt") ||
                fileName.toLowerCase().endsWith(".md") ||
                fileName.toLowerCase().endsWith(".pdf") ||
                fileName.toLowerCase().endsWith(".word");
    }

}
