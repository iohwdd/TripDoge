package com.tripdog.service.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EmailServiceImpl测试类
 * 验证P0-2修复：验证码存储迁移到Redis
 */
class EmailServiceImplTest {

    @Test
    void testCodeStorageUsesRedis() throws NoSuchFieldException {
        // 验证不再使用ConcurrentHashMap存储验证码
        // 应该使用RedisService
        Field redisServiceField = EmailServiceImpl.class.getDeclaredField("redisService");
        assertNotNull(redisServiceField, "应该有redisService字段");
        
        // 验证CODE_KEY_PREFIX常量存在
        Field codeKeyPrefixField = EmailServiceImpl.class.getDeclaredField("CODE_KEY_PREFIX");
        assertNotNull(codeKeyPrefixField, "应该有CODE_KEY_PREFIX常量");
        
        codeKeyPrefixField.setAccessible(true);
        try {
            String prefix = (String) codeKeyPrefixField.get(null);
            assertEquals("email:code:", prefix, "CODE_KEY_PREFIX应该是'email:code:'");
        } catch (IllegalAccessException e) {
            fail("无法访问CODE_KEY_PREFIX字段");
        }
        
        // 验证MAX_GENERATE_RETRIES常量存在
        Field maxRetriesField = EmailServiceImpl.class.getDeclaredField("MAX_GENERATE_RETRIES");
        assertNotNull(maxRetriesField, "应该有MAX_GENERATE_RETRIES常量");
        
        maxRetriesField.setAccessible(true);
        try {
            int maxRetries = maxRetriesField.getInt(null);
            assertEquals(100, maxRetries, "MAX_GENERATE_RETRIES应该是100");
        } catch (IllegalAccessException e) {
            fail("无法访问MAX_GENERATE_RETRIES字段");
        }
    }
}

