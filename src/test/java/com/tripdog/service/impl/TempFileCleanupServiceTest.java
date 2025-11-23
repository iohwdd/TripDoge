package com.tripdog.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TempFileCleanupService测试类
 * 验证P0-5修复：临时文件清理机制
 */
class TempFileCleanupServiceTest {

    @Test
    void testCleanupTempFilesHasScheduledAnnotation() throws NoSuchMethodException {
        // 验证cleanupTempFiles方法有@Scheduled注解
        Method cleanupMethod = TempFileCleanupService.class.getMethod("cleanupTempFiles");
        
        assertTrue(cleanupMethod.isAnnotationPresent(Scheduled.class), 
            "cleanupTempFiles方法应该有@Scheduled注解");
        
        Scheduled scheduled = cleanupMethod.getAnnotation(Scheduled.class);
        assertNotNull(scheduled, "Scheduled注解应该存在");
        
        // 验证fixedDelay配置（6小时 = 6 * 60 * 60 * 1000毫秒）
        long fixedDelay = scheduled.fixedDelay();
        assertEquals(6 * 60 * 60 * 1000L, fixedDelay, 
            "fixedDelay应该是6小时（21600000毫秒）");
    }

    @Test
    void testCleanupFileMethodExists() throws NoSuchMethodException {
        // 验证cleanupFile方法存在
        Method cleanupFileMethod = TempFileCleanupService.class.getMethod("cleanupFile", String.class);
        assertNotNull(cleanupFileMethod, "cleanupFile方法应该存在");
    }
}


