package com.tripdog.integration;

import com.tripdog.common.utils.FileValidationUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 文件验证集成测试
 * 验证P0-3修复：文件大小和类型限制
 */
class FileValidationIntegrationTest {

    @Test
    void testDocumentUpload_ValidFile() {
        // 测试有效的文档文件上传
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            new byte[50 * 1024 * 1024] // 50MB，在限制内
        );
        
        assertDoesNotThrow(() -> {
            FileValidationUtils.validateDocumentFile(file);
        }, "50MB的PDF文件应该通过验证");
    }

    @Test
    void testDocumentUpload_FileTooLarge() {
        // 测试文件过大
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            new byte[101 * 1024 * 1024] // 101MB，超过100MB限制
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidationUtils.validateDocumentFile(file),
            "超过100MB的文件应该抛出异常"
        );
        
        assertTrue(exception.getMessage().contains("文件大小超过限制"), 
            "异常消息应该包含'文件大小超过限制'");
    }

    @Test
    void testDocumentUpload_InvalidExtension() {
        // 测试不支持的文件扩展名
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.exe",
            "application/x-msdownload",
            new byte[1024]
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidationUtils.validateDocumentFile(file),
            "不支持的文件类型应该抛出异常"
        );
        
        assertTrue(exception.getMessage().contains("不支持的文件类型"), 
            "异常消息应该包含'不支持的文件类型'");
    }

    @Test
    void testImageUpload_ValidImage() {
        // 测试有效的图片文件上传
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            new byte[5 * 1024 * 1024] // 5MB，在限制内
        );
        
        assertDoesNotThrow(() -> {
            FileValidationUtils.validateImageFile(file);
        }, "5MB的图片文件应该通过验证");
    }

    @Test
    void testImageUpload_ImageTooLarge() {
        // 测试图片过大
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "large.jpg",
            "image/jpeg",
            new byte[11 * 1024 * 1024] // 11MB，超过10MB限制
        );
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FileValidationUtils.validateImageFile(file),
            "超过10MB的图片应该抛出异常"
        );
        
        assertTrue(exception.getMessage().contains("图片大小超过限制"), 
            "异常消息应该包含'图片大小超过限制'");
    }
}



