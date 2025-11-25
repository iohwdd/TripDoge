package com.tripdog.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件验证工具类
 * 提供文件大小、类型等验证功能
 */
@Slf4j
public class FileValidationUtils {

    /**
     * 文档上传允许的文件类型（MIME类型）
     */
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = new HashSet<>(Arrays.asList(
        // PDF
        "application/pdf",
        // Word文档
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        // Excel文档
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        // PowerPoint文档
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        // 文本文件
        "text/plain",
        "text/markdown",
        "text/html",
        // 其他常见文档格式
        "application/rtf",
        "application/x-rtf"
    ));

    /**
     * 文档上传允许的文件扩展名
     */
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".txt", ".md", ".html", ".rtf"
    ));

    /**
     * 图片上传允许的文件类型（MIME类型）
     */
    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    ));

    /**
     * 图片上传允许的文件扩展名
     */
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    ));

    /**
     * 文档上传最大文件大小（100MB）
     */
    private static final long MAX_DOCUMENT_SIZE = 100 * 1024 * 1024; // 100MB

    /**
     * 图片上传最大文件大小（10MB）
     */
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * 验证文档文件
     *
     * @param file 上传的文件
     * @throws IllegalArgumentException 如果文件不符合要求
     */
    public static void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException(
                String.format("文件大小超过限制（最大%dMB）", MAX_DOCUMENT_SIZE / (1024 * 1024))
            );
        }

        // 验证文件类型
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名无效或缺少扩展名");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        // 检查扩展名
        if (!ALLOWED_DOCUMENT_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                String.format("不支持的文件类型: %s，支持的格式: %s", 
                    extension, String.join(", ", ALLOWED_DOCUMENT_EXTENSIONS))
            );
        }

        // 检查MIME类型（如果提供）
        if (contentType != null && !ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("文件MIME类型与扩展名不匹配: contentType={}, extension={}, filename={}", 
                contentType, extension, originalFilename);
            // 不直接拒绝，因为某些浏览器可能发送错误的MIME类型
            // 主要依赖扩展名验证
        }

        log.debug("文档文件验证通过: filename={}, size={}, contentType={}", 
            originalFilename, file.getSize(), contentType);
    }

    /**
     * 验证图片文件
     *
     * @param file 上传的文件
     * @throws IllegalArgumentException 如果文件不符合要求
     */
    public static void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 验证文件大小
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                String.format("图片大小超过限制（最大%dMB）", MAX_IMAGE_SIZE / (1024 * 1024))
            );
        }

        // 验证文件类型
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("文件名无效或缺少扩展名");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        // 检查扩展名
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                String.format("不支持的图片类型: %s，支持的格式: %s", 
                    extension, String.join(", ", ALLOWED_IMAGE_EXTENSIONS))
            );
        }

        // 检查MIME类型（如果提供）
        if (contentType != null && !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            log.warn("图片MIME类型与扩展名不匹配: contentType={}, extension={}, filename={}", 
                contentType, extension, originalFilename);
            // 不直接拒绝，因为某些浏览器可能发送错误的MIME类型
        }

        log.debug("图片文件验证通过: filename={}, size={}, contentType={}", 
            originalFilename, file.getSize(), contentType);
    }

    /**
     * 获取文档上传最大文件大小
     */
    public static long getMaxDocumentSize() {
        return MAX_DOCUMENT_SIZE;
    }

    /**
     * 获取图片上传最大文件大小
     */
    public static long getMaxImageSize() {
        return MAX_IMAGE_SIZE;
    }

    /**
     * 获取允许的文档扩展名
     */
    public static Set<String> getAllowedDocumentExtensions() {
        return new HashSet<>(ALLOWED_DOCUMENT_EXTENSIONS);
    }

    /**
     * 获取允许的图片扩展名
     */
    public static Set<String> getAllowedImageExtensions() {
        return new HashSet<>(ALLOWED_IMAGE_EXTENSIONS);
    }
}



